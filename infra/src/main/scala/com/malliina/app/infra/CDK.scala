package com.malliina.app.infra

import software.amazon.awscdk.services.ec2.{IVpc, Vpc, VpcAttributes, VpcLookupOptions}
import software.amazon.awscdk.{Environment, StackProps}
import software.amazon.awscdk.{Environment, Stack, StackProps, App as AWSApp}
import software.constructs.Construct

object CDK:
  val stackProps =
    StackProps
      .builder()
      .env(Environment.builder().account("297686094835").region("eu-west-1").build())
      .build()

  def main(args: Array[String]): Unit =
    val app = new AWSApp()
    val vpc = VPCStack(app, "refvpc", VPCStack.CIDRs.default).vpc
//    val qa = AppEnv(app, "ref", Env.Qa, vpc, Seq("sg-0e1724683903cd691"))
    val qa = AppEnvLookupVpc(
      app,
      "ref",
      Env.Qa,
      vpcId = "vpc-00cee1dac3e54ef13",
      securityGroupIds = Seq("sg-0e1724683903cd691") // group with access to database
    )
    val assembly = app.synth()

enum Env(val name: String):
  override def toString = name
  case Qa extends Env("qa")

class AppEnv(scope: Construct, appName: String, env: Env, vpc: IVpc, securityGroupIds: Seq[String])
  extends Stack(scope, s"$env-$appName", CDK.stackProps):
  val pipeline = BeanstalkPipeline(this, s"$env-$appName", vpc, securityGroupIds, None)

class AppEnvLookupVpc(
  scope: Construct,
  appName: String,
  env: Env,
  vpcId: String,
  securityGroupIds: Seq[String]
) extends Stack(scope, s"$env-$appName", CDK.stackProps):
  val vpcReference = Vpc.fromLookup(
    this,
    "vpc-lookup",
    VpcLookupOptions.builder().vpcId(vpcId).build()
  )
  val pipeline =
    BeanstalkPipeline(this, s"$env-$appName", vpcReference, securityGroupIds, database = None)
