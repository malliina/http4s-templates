package com.malliina.app.infra

import software.amazon.awscdk.{RemovalPolicy, Stack}
import software.amazon.awscdk.services.codebuild.*
import software.amazon.awscdk.services.codecommit.Repository
import software.amazon.awscdk.services.codepipeline.actions.{CodeBuildAction, CodeCommitSourceAction, ElasticBeanstalkDeployAction}
import software.amazon.awscdk.services.codepipeline.{Artifact, IAction, Pipeline, StageProps}
import software.amazon.awscdk.services.ec2.{IVpc, Vpc, VpcLookupOptions}
import software.amazon.awscdk.services.elasticbeanstalk.{CfnApplication, CfnConfigurationTemplate, CfnEnvironment}
import software.amazon.awscdk.services.iam.{CfnInstanceProfile, ManagedPolicy, PolicyDocument, Role}
import software.amazon.awscdk.services.s3.{BlockPublicAccess, Bucket}

import scala.jdk.CollectionConverters.ListHasAsScala

object BeanstalkPipeline:
  case class Network(
    vpcId: String,
    privateSubnetIds: Seq[String],
    publicSubnetIds: Seq[String],
    elbSecurityGroupId: String
  )

class BeanstalkPipeline(stack: Stack, prefix: String, vpc: IVpc) extends CDKBuilders:
  val region = stack.getRegion
  val account = stack.getAccount
  val envName = prefix
  val app = CfnApplication.Builder
    .create(stack, "MyCdkBeanstalk")
    .applicationName(envName)
    .description("Built with CDK in Helsinki")
    .build()
  val appName = app.getApplicationName
  val solutionStack = "64bit Amazon Linux 2 v3.4.1 running Corretto 17"

  val branch = "master"

  val architecture: Arch = Arch.Arm64

  def buildImage: IBuildImage = architecture match
    case Arch.Arm64  => LinuxArmBuildImage.AMAZON_LINUX_2_STANDARD_2_0
    case Arch.X86_64 => LinuxBuildImage.AMAZON_LINUX_2_4
    case Arch.I386   => LinuxBuildImage.STANDARD_6_0

  def buildComputeType = architecture match
    case Arch.Arm64 => ComputeType.SMALL
    case _          => ComputeType.MEDIUM

  val serviceRole = Role.Builder
    .create(stack, "ServiceRole")
    .assumedBy(principal("elasticbeanstalk.amazonaws.com"))
    .managedPolicies(
      list(
        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSElasticBeanstalkEnhancedHealth"),
        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSElasticBeanstalkService")
      )
    )
    .build()
  val appRole = Role.Builder
    .create(stack, "AppRole")
    .assumedBy(principal("ec2.amazonaws.com"))
    .managedPolicies(
      list(
        ManagedPolicy.fromAwsManagedPolicyName("AWSElasticBeanstalkWebTier")
      )
    )
    .build()
  val instanceProfile = CfnInstanceProfile.Builder
    .create(stack, "InstanceProfile")
    .path("/")
    .roles(list(appRole.getRoleName))
    .build()
  val configurationTemplate = CfnConfigurationTemplate.Builder
    .create(stack, "BeanstalkConfigurationTemplate")
    .applicationName(appName)
    .solutionStackName(solutionStack)
    .optionSettings(
      list[AnyRef](
        optionSetting(
          "aws:autoscaling:launchconfiguration",
          "IamInstanceProfile",
          instanceProfile.getRef
        ),
        optionSetting("aws:elasticbeanstalk:environment", "ServiceRole", serviceRole.getRoleName),
        ebEnvVar("PORT", "9000"),
//        ebEnvVar(
//          "APPLICATION_SECRET",
//          "{{resolve:secretsmanager:dev/refapp/secrets:SecretString:appsecret}}"
//        ),
        asgMinSize(1),
        asgMaxSize(2),
        ebEnvironment("EnvironmentType", "LoadBalanced"),
        ebEnvironment("LoadBalancerType", "application"),
        //        optionSetting(
        //          "aws:elasticbeanstalk:environment:process:default",
        //          "HealthCheckPath",
        //          "/health"
        //        ),
        optionSetting(
          "aws:elasticbeanstalk:environment:process:default",
          "StickinessEnabled",
          "true"
        ),
        ebVpc("VPCId", vpc.getVpcId),
        ebSubnets(vpc.getPrivateSubnets.asScala.toList),
        ebElbSubnets(vpc.getPublicSubnets.asScala.toList),
        ebInstanceType(t4g.small),
        ebDeployment("DeploymentPolicy", "AllAtOnce"),
        supportedArchitectures(Seq(architecture)),
        streamLogs
      )
    )
    .build()
  val beanstalkEnv = CfnEnvironment.Builder
    .create(stack, "Env")
    .applicationName(appName)
    .environmentName(envName)
    .templateName(configurationTemplate.getRef)
    .build()

  // Pipeline

  val buildEnv =
    BuildEnvironment
      .builder()
      .buildImage(buildImage)
      .computeType(buildComputeType)
      .build()
  val codebuildProject =
    PipelineProject.Builder
      .create(stack, "Build")
      .buildSpec(BuildSpec.fromSourceFilename("buildspec-java.yml"))
      .environment(buildEnv)
      .build()
  val repo = Repository.Builder
    .create(stack, "Code")
    .repositoryName(envName)
    .description(s"Repository for $envName environment of app $appName.")
    .build()
  val sourceOut = new Artifact()
  val buildOut = new Artifact()
  val pipelineRole = Role.Builder
    .create(stack, "PipelineRole")
    .assumedBy(principal("codepipeline.amazonaws.com"))
    .managedPolicies(
      list(
        ManagedPolicy.fromAwsManagedPolicyName("AWSCodePipeline_FullAccess")
      )
    )
    .build()
  repo.grantPullPush(pipelineRole)
  val artifactsBucket = Bucket.Builder
    .create(stack, "Artifacts")
    .bucketName(s"$envName-artifacts")
    .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
    .removalPolicy(RemovalPolicy.DESTROY)
    .autoDeleteObjects(true)
    .build()
  val pipeline: Pipeline = Pipeline.Builder
    .create(stack, "Pipeline")
    .role(pipelineRole)
    .artifactBucket(artifactsBucket)
    .stages(
      list[StageProps](
        stage("Source")(
          CodeCommitSourceAction.Builder
            .create()
            .actionName("SourceAction")
            .repository(repo)
            .branch(branch)
            .output(sourceOut)
            .build()
        ),
        stage("Build")(
          CodeBuildAction.Builder
            .create()
            .actionName("BuildAction")
            .project(codebuildProject)
            .input(sourceOut)
            .outputs(list(buildOut))
            .build()
        ),
        stage("Deploy")(
          ElasticBeanstalkDeployAction.Builder
            .create()
            .actionName("DeployAction")
            .applicationName(appName)
            .environmentName(beanstalkEnv.getEnvironmentName)
            .input(buildOut)
            .build()
        )
      )
    )
    .build()
