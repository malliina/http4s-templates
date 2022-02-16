package com.malliina.app.infra

import software.amazon.awscdk.services.certificatemanager.Certificate
import software.amazon.awscdk.{Duration, RemovalPolicy, Stack}
import software.amazon.awscdk.services.cognito.{AccountRecovery, AttributeMapping, AuthFlow, ClientAttributes, CognitoDomainOptions, CustomDomainOptions, Mfa, OAuthFlows, OAuthScope, OAuthSettings, ProviderAttribute, SignInAliases, UserPool, UserPoolClientIdentityProvider, UserPoolClientOptions, UserPoolDomainOptions, UserPoolEmail, UserPoolIdentityProviderGoogle}
import software.amazon.awscdk.services.ssm.StringParameter
import software.constructs.Construct

object Cognito:
  def stack(construct: Construct, name: String, env: Env): Cognito =
    val s = Stack(construct, s"$env-$name", CDK.stackProps)
    Cognito(s, name, env)

/** Creates a Cognito user pool with Google as an identity provider (social login).
  *
  * One problem with this setup is
  * https://stackoverflow.com/questions/58154256/aws-cognito-how-to-force-select-account-when-signing-in-with-google.
  */
class Cognito(stack: Stack, name: String, env: Env) extends CDKBuilders:
  val userPool = UserPool.Builder
    .create(stack, "UserPool")
    .userPoolName(s"$env-$name")
    .mfa(Mfa.OFF)
    .email(UserPoolEmail.withCognito())
    .selfSignUpEnabled(true)
    .signInAliases(SignInAliases.builder().email(true).build())
    .signInCaseSensitive(false)
    .accountRecovery(AccountRecovery.EMAIL_AND_PHONE_WITHOUT_MFA)
    .removalPolicy(RemovalPolicy.DESTROY)
    .build()
  val google = UserPoolIdentityProviderGoogle.Builder
    .create(stack, "Google")
    .userPool(userPool)
    .scopes(list("email"))
    .clientId(StringParameter.valueForStringParameter(stack, "/google/id", 1))
    .clientSecret(StringParameter.valueForStringParameter(stack, "/google/secret", 1))
    .attributeMapping(
      AttributeMapping
        .builder()
        .email(ProviderAttribute.GOOGLE_EMAIL)
        .custom(
          map(
            "email_verified" -> ProviderAttribute.other("email_verified"),
            "username" -> ProviderAttribute.other("sub")
          )
        )
        .build()
    )
    .build()
  val domain = userPool.addDomain(
    "Domain",
    UserPoolDomainOptions
      .builder()
//      .customDomain(
//        CustomDomainOptions
//          .builder()
//          .domainName("auth.malliina.site")
//          .certificate(Certificate.fromCertificateArn(stack, "Cert", "arn"))
//          .build()
//      )
      .cognitoDomain(CognitoDomainOptions.builder().domainPrefix(s"$env-$name-domain").build())
      .build()
  )
  val app = userPool.addClient(
    "Client",
    UserPoolClientOptions
      .builder()
      .userPoolClientName("app")
      .generateSecret(true)
      .oAuth(
        OAuthSettings
          .builder()
          .scopes(list(OAuthScope.OPENID, OAuthScope.EMAIL))
          .flows(OAuthFlows.builder().authorizationCodeGrant(true).build())
          .callbackUrls(list("http://localhost:9000/auth/login/callback"))
          .logoutUrls(list("http://localhost:9000/auth/logout/callback"))
          .build()
      )
      .authFlows(AuthFlow.builder().userSrp(true).build())
      .accessTokenValidity(Duration.minutes(60))
      .idTokenValidity(Duration.minutes(60))
      .refreshTokenValidity(Duration.days(3650))
      .supportedIdentityProviders(list(UserPoolClientIdentityProvider.GOOGLE))
      .preventUserExistenceErrors(true)
      .build()
  )
  app.getNode.addDependency(google)
