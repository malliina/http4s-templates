package com.malliina.app.infra

import software.amazon.awscdk.services.ec2.IVpc
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
    val vpc = VPCStack(app, "refvpc", VPCStack.CIDRs.default)
    val qa = AppEnv(app, "mini", Env.Qa, vpc.vpc)
    val assembly = app.synth()

enum Env(val name: String):
  override def toString = name
  case Qa extends Env("qa")

class AppEnv(scope: Construct, appName: String, env: Env, vpc: IVpc)
  extends Stack(scope, s"$env-$appName", CDK.stackProps):
  val pipeline = BeanstalkPipeline(this, s"$env-$appName", vpc)
