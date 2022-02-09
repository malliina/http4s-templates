package com.malliina.app.infra

import software.amazon.awscdk.{Environment, StackProps}
import software.amazon.awscdk.{Environment, Stack, StackProps, App as AWSApp}

object CDK:
  val stackProps =
    StackProps
      .builder()
      .env(Environment.builder().account("297686094835").region("eu-west-1").build())
      .build()

  def main(args: Array[String]): Unit =
    val app = new AWSApp()
