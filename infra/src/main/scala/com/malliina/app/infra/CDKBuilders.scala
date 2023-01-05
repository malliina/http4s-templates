package com.malliina.app.infra

import software.amazon.awscdk.services.codepipeline.{IAction, StageProps}
import software.amazon.awscdk.{CfnOutput, Stack}
import software.amazon.awscdk.services.ec2.ISubnet
import software.amazon.awscdk.services.elasticbeanstalk.CfnConfigurationTemplate.ConfigurationOptionSettingProperty
import software.amazon.awscdk.services.iam.{Effect, PolicyDocument, PolicyStatement, ServicePrincipal}

import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}

trait CDKBuilders extends OptionSettings:
  def principal(service: String) = ServicePrincipal.Builder.create(service).build()
  def list[T](xs: T*) = xs.asJava
  def map[T](kvs: (String, T)*) = Map(kvs*).asJava
  def outputs(scope: Stack)(kvs: (String, String)*) = kvs.map { case (k, v) =>
    CfnOutput.Builder
      .create(scope, k)
      .exportName(k)
      .value(v)
      .build()
  }
  def policy(statements: PolicyStatement*) = PolicyDocument.Builder
    .create()
    .statements(list(statements*))
    .build()
  def allowAction(action: String, arn: String, moreArns: String*) =
    allowStatement(Seq(action), Seq(arn) ++ moreArns)
  def allowStatement(actions: Seq[String], arns: Seq[String]) =
    PolicyStatement.Builder
      .create()
      .actions(list(actions*))
      .effect(Effect.ALLOW)
      .resources(list(arns*))
      .build()
  def stage(name: String)(actions: IAction*) =
    StageProps.builder().stageName(name).actions(list(actions*)).build()
  def secretJson(secretName: String, jsonKey: String) =
    s"{{resolve:secretsmanager:$secretName::$jsonKey}}"
