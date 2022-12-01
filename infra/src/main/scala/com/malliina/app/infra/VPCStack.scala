package com.malliina.app.infra

import com.malliina.app.infra.VPCStack.CIDRs
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.ec2.{IIpAddresses, IpAddresses, Vpc}
import software.constructs.Construct

import scala.jdk.CollectionConverters.ListHasAsScala

object VPCStack:
  case class CIDRs(
    vpc: String,
    public1: String,
    public2: String,
    private1: String,
    private2: String
  )
  object CIDRs:
    val default =
      CIDRs("10.50.0.0/16", "10.50.0.0/24", "10.50.1.0/24", "10.50.64.0/19", "10.50.96.0/19")
    val default2 =
      CIDRs(
        "172.16.0.0/16",
        "172.16.64.0/24",
        "172.16.128.0/24",
        "172.16.64.0/19",
        "172.16.96.0/19"
      )

class VPCStack(scope: Construct, stackName: String, cidrs: CIDRs)
  extends Stack(scope, stackName, CDK.stackProps)
  with CDKBuilders:
  val stack = this

  /** This creates a VPC along with private, public subnets, etc networking resources.
    */
  val vpc = Vpc.Builder
    .create(stack, "VPC")
    .ipAddresses(IpAddresses.cidr(cidrs.vpc))
    .enableDnsSupport(true)
    .enableDnsHostnames(true)
    .maxAzs(2)
    .build()
  val azs = vpc.getAvailabilityZones.asScala
  val az1 = azs(0)
  val az2 = azs(1)

  outputs(stack)(
    "VPCId" -> vpc.getVpcId,
    "VPCArn" -> vpc.getVpcArn,
    "PrivateSubnetCIDRs" -> vpc.getPrivateSubnets.asScala.map(_.getIpv4CidrBlock).mkString(", "),
    "PublicSubnetCIDRs" -> vpc.getPublicSubnets.asScala.map(_.getIpv4CidrBlock).mkString(", ")
  )
