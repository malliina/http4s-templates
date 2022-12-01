# http4s-app

This is an [http4s](https://http4s.org/) project template.

    sbt
    ~server/reStart

Navigate to http://localhost:9000.

| Endpoint  | Returns                                                  |
|-----------|----------------------------------------------------------|
| /health   | An example OK 200 response                               |
| /assets/* | Assets from [server/assets/public](server/assets/public) |

For example, [kopp-small.jpg](server/assets/public/kopp-small.jpg) is available at http://localhost:9000/assets/kopp-small.jpg.

## Repository Structure

| Folder           | Contains                                                 |
|------------------|----------------------------------------------------------|
| [server](server) | http4s server                                            |
| [infra](infra)   | Infrastructure in [AWS CDK](https://aws.amazon.com/cdk/) |

Infra code for Azure is [here](https://github.com/malliina/bicep/blob/master/javawebapp.bicep).

## Deployment

Setup infrastructure:

    cdk boostrap
    cdk deploy refvpc
    cdk deploy qa-ref
