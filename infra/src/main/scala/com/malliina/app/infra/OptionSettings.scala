package com.malliina.app.infra

import software.amazon.awscdk.services.ec2.ISubnet
import software.amazon.awscdk.services.elasticbeanstalk.CfnConfigurationTemplate.ConfigurationOptionSettingProperty

trait OptionSettings:
  def asgMinSize(size: Int) = optionSetting("aws:autoscaling:asg", "MinSize", s"$size")
  def asgMaxSize(size: Int) = optionSetting("aws:autoscaling:asg", "MaxSize", s"$size")
  def ebEnvironment(optionName: String, value: String) =
    optionSetting("aws:elasticbeanstalk:environment", optionName, value)
  def ebSubnets(subnets: Seq[ISubnet]) =
    ebVpc("Subnets", subnets.map(_.getSubnetId).mkString(","))
  def ebElbSubnets(subnets: Seq[ISubnet]) =
    ebVpc("ELBSubnets", subnets.map(_.getSubnetId).mkString(","))
  def ebSecurityGroupIds(ids: Seq[String]) =
    optionSetting(
      "aws:autoscaling:launchconfiguration",
      "SecurityGroups",
      ids.mkString(",")
    )
  def ebVpc(optionName: String, value: String) =
    optionSetting("aws:ec2:vpc", optionName, value)
  def ebDeployment(optionName: String, value: String) =
    optionSetting("aws:elasticbeanstalk:command", optionName, value)
  def ebInstanceType(instance: String) = ebInstanceTypes(Seq(instance))
  def ebInstanceTypes(instanceTypes: Seq[String]) =
    ebInstances("InstanceTypes", instanceTypes.mkString(", "))
  def supportedArchitectures(architectures: Seq[Arch]) =
    ebInstances("SupportedArchitectures", architectures.mkString(", "))
  def ebInstances(optionName: String, value: String) =
    optionSetting("aws:ec2:instances", optionName, value)
  def ebEnvVar(key: String, value: String) =
    optionSetting("aws:elasticbeanstalk:application:environment", key, value)
  def streamLogs = ebLogs("StreamLogs", "true")
  def deleteLogsOnTerminate = ebLogs("DeleteOnTerminate", "true")
  def ebLogs(optionName: String, value: String) =
    optionSetting("aws:elasticbeanstalk:cloudwatch:logs", optionName, value)
  def optionSetting(namespace: String, optionName: String, value: String) =
    ConfigurationOptionSettingProperty
      .builder()
      .namespace(namespace)
      .optionName(optionName)
      .value(value)
      .build()
