package com.malliina.app.infra

import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.codebuild.*
import software.amazon.awscdk.services.codecommit.Repository
import software.amazon.awscdk.services.codepipeline.actions.{CodeBuildAction, CodeCommitSourceAction}
import software.amazon.awscdk.services.codepipeline.{Artifact, IAction, Pipeline, StageProps}
import software.amazon.awscdk.services.ec2.{IVpc, Vpc, VpcLookupOptions}
import software.amazon.awscdk.services.elasticbeanstalk.{CfnApplication, CfnConfigurationTemplate, CfnEnvironment}
import software.amazon.awscdk.services.iam.{CfnInstanceProfile, ManagedPolicy, Role}

import scala.jdk.CollectionConverters.ListHasAsScala

object BeanstalkPipeline:
  case class Network(
    vpcId: String,
    privateSubnetIds: Seq[String],
    publicSubnetIds: Seq[String],
    elbSecurityGroupId: String
  )

class BeanstalkPipeline(stack: Stack, prefix: String, vpc: IVpc) extends CDKBuilders:
  val envName = s"$prefix-app"
  val app = CfnApplication.Builder
    .create(stack, "MyCdkBeanstalk")
    .applicationName(envName)
    .description("Built with CDK in Helsinki")
    .build()
  val appName = app.getApplicationName
  val namePrefix = "MyCdk"
  val dockerSolutionStackName = "64bit Amazon Linux 2 v3.1.0 running Docker"
  val javaSolutionStackName = "64bit Amazon Linux 2 v3.2.8 running Corretto 11"
  val solutionStack = javaSolutionStackName

  val branch = "master"

  val architecture: Arch = Arch.Arm64

  def buildImage: IBuildImage = architecture match
    case Arch.Arm64  => LinuxBuildImage.AMAZON_LINUX_2_ARM_2
    case Arch.X86_64 => LinuxBuildImage.AMAZON_LINUX_2_3
    case Arch.I386   => LinuxBuildImage.STANDARD_5_0

  def buildComputeType = architecture match
    case Arch.Arm64 => ComputeType.SMALL
    case _          => ComputeType.MEDIUM

  def makeId(name: String) = s"$envName-$name"

  // Environment

  val serviceRole = Role.Builder
    .create(stack, makeId("ServiceRole"))
    .assumedBy(principal("elasticbeanstalk.amazonaws.com"))
    .managedPolicies(
      list(
        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSElasticBeanstalkEnhancedHealth"),
        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSElasticBeanstalkService")
      )
    )
    .build()
  val appRole = Role.Builder
    .create(stack, makeId("AppRole"))
    .assumedBy(principal("ec2.amazonaws.com"))
    .managedPolicies(
      list(
        ManagedPolicy.fromAwsManagedPolicyName("AWSElasticBeanstalkWebTier"),
        ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess")
      )
    )
    .build()
  val instanceProfile = CfnInstanceProfile.Builder
    .create(stack, makeId("InstanceProfile"))
    .path("/")
    .roles(list(appRole.getRoleName))
    .build()
  val configurationTemplate = CfnConfigurationTemplate.Builder
    .create(stack, makeId("BeanstalkConfigurationTemplate"))
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
        ebEnvVar(
          "APPLICATION_SECRET",
          "{{resolve:secretsmanager:dev/refapp/secrets:SecretString:appsecret}}"
        ),
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
        supportedArchitectures(Seq(Arch.Arm64)),
        streamLogs
      )
    )
    .build()
  val beanstalkEnv = CfnEnvironment.Builder
    .create(stack, makeId("Env"))
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
  val buildSpec =
    if solutionStack == javaSolutionStackName then "buildspec-java.yml" else "buildspec.yml"
  val codebuildProject =
    PipelineProject.Builder
      .create(stack, makeId("Build"))
      .buildSpec(BuildSpec.fromSourceFilename(buildSpec))
      .environment(buildEnv)
      .build()
  val repo = Repository.Builder
    .create(stack, makeId("Code"))
    .repositoryName(makeId("Repo"))
    .description(s"Repository for $envName environment of app $appName.")
    .build()
  val sourceOut = new Artifact()
  val buildOut = new Artifact()
  val pipelineRole = Role.Builder
    .create(stack, makeId("PipelineRole"))
    .assumedBy(principal("codepipeline.amazonaws.com"))
    .managedPolicies(
      list(
        ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess"),
        ManagedPolicy.fromAwsManagedPolicyName("AWSCodeCommitFullAccess"),
        ManagedPolicy.fromAwsManagedPolicyName("AWSCodePipelineFullAccess"),
        ManagedPolicy.fromAwsManagedPolicyName("AdministratorAccess-AWSElasticBeanstalk")
      )
    )
    .build()
  val pipeline: Pipeline = Pipeline.Builder
    .create(stack, makeId("Pipeline"))
    .role(pipelineRole)
    .stages(
      list[StageProps](
        StageProps
          .builder()
          .stageName("Source")
          .actions(
            list[IAction](
              CodeCommitSourceAction.Builder
                .create()
                .actionName("SourceAction")
                .repository(repo)
                .branch(branch)
                .output(sourceOut)
                .build()
            )
          )
          .build(),
        StageProps
          .builder()
          .stageName("Build")
          .actions(
            list[IAction](
              CodeBuildAction.Builder
                .create()
                .actionName("BuildAction")
                .project(codebuildProject)
                .input(sourceOut)
                .outputs(list(buildOut))
                .build()
            )
          )
          .build(),
        StageProps
          .builder()
          .stageName("Deploy")
          .actions(
            list[IAction](
              new BeanstalkDeployAction(
                EBDeployActionData(
                  "DeployAction",
                  buildOut,
                  appName,
                  beanstalkEnv.getEnvironmentName
                )
              )
            )
          )
          .build()
      )
    )
    .build()
