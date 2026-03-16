# ApiSpec CRD — CI/CD Integration Guide

This guide explains how to integrate the ApiSpec CRD and Winspect operator into your CI/CD pipeline for GitOps-friendly API discovery.

## Overview

1. Add `api-spec.yaml` to your service repository
2. In your CD pipeline, run `winspect resolve` to resolve tokens and embed spec content
3. Apply the resolved manifest: `kubectl apply -f -`
4. The Winspect operator (installed in the cluster) watches ApiSpec CRDs and syncs to the central platform

## Step 1: Add api-spec.yaml to Your Repo

Create `api-spec.yaml` in your service repository root (or a subdirectory):

```yaml
apiVersion: winspect.io/v1
kind: ApiSpec
metadata:
  name: order-service
  namespace: e-commerce
spec:
  owner: "team-payments"       # Team name or UUID
  type: "openapi-3.1"
  gitSha: "${SPEC_SHA}"       # Resolved by winspect resolve
  githubRepo: "https://github.com/myorg/order-service"
  githubBranch: "main"
  specFilePath: "openapi.json"
  definition:
    ref: "./openapi.json"     # CLI resolves this and embeds content
```

**Tokens (resolved by `winspect resolve`):**
- `${SPEC_SHA}` — Last commit of the file in `definition.ref`
- `${GIT_SHA}` — Current HEAD
- `${BRANCH}` — Current branch

## Step 2: CI/CD Pipeline Integration

### GitHub Actions

```yaml
- name: Resolve and apply ApiSpec
  run: |
    winspect resolve --spec api-spec.yaml | kubectl apply -f -
  env:
    KUBECONFIG: ${{ secrets.KUBE_CONFIG }}
```

### Argo CD / Flux

1. Use a pre-sync hook or custom tool to run `winspect resolve` before apply
2. Or: run `winspect resolve` in CI and commit the resolved manifest to a separate "deployed" branch that Argo/Flux syncs from

### Generic CI (e.g. Jenkins, GitLab CI)

```bash
winspect resolve --spec api-spec.yaml --output resolved-apispec.yaml
kubectl apply -f resolved-apispec.yaml
```

## Step 3: Ensure Operator is Installed

The Winspect ApiSpec controller must be running in the cluster. It watches ApiSpec CRDs and syncs to the central platform. Configure it with:

- `APISPEC_ORG_ID`
- `APISPEC_API_KEY`
- `APISPEC_CENTRAL_API_URL`

## Large Specs (ConfigMapRef)

For OpenAPI specs larger than ~50KB, use `configMapRef` instead of inline:

1. In CI, create a ConfigMap with the spec content
2. Reference it in ApiSpec:

```yaml
spec:
  definition:
    configMapRef:
      name: order-service-openapi
      key: spec.yaml
```

Your pipeline would create the ConfigMap and ApiSpec in the same apply.

## Troubleshooting

- **Spec not syncing:** Check operator logs and that `orgId`, `apiKey`, `centralApiUrl` are set
- **Token not resolved:** Ensure `winspect resolve` runs in a git repository and `definition.ref` points to an existing file
- **ConfigMap not found:** Ensure the ConfigMap is in the same namespace as the ApiSpec
