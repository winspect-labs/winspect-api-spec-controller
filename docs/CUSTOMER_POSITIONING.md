# Winspect API Discovery — Customer Positioning

## Dual Approach

Winspect offers two approaches for discovering and registering APIs. Choose based on your workflow and constraints.

| Approach | When to Use | Pros |
|----------|-------------|------|
| **ApiSpec CRD + Operator** (primary) | GitOps, spec in repo, CD deploys CRD | Source of truth in git, versioned, diff links, no runtime probing |
| **Runtime discovery** (fallback) | Legacy services, no CRD in repo, quick onboarding | Zero config, works with any service exposing OpenAPI |

## ApiSpec CRD + Operator (Primary)

**Best for:** Teams that store OpenAPI specs in git and use GitOps (Argo CD, Flux) or CI/CD to deploy.

**Benefits:**
- **Source of truth in git** — Spec is version-controlled with your code
- **GitHub diff links** — Jump from Winspect UI to the exact spec version in GitHub
- **No runtime probing** — No need to call live services; spec is applied at deploy time
- **GitOps-friendly** — Declarative, fits Argo CD / Flux workflows

**Requirements:**
- Add `api-spec.yaml` to your service repo
- Run `winspect resolve` in CI/CD before apply
- Install Winspect ApiSpec controller in the cluster

## Runtime Discovery (Fallback)

**Best for:** Legacy services, quick onboarding, or when you cannot add ApiSpec to the repo.

**Benefits:**
- **Zero config** — No manifest to add; agent discovers services automatically
- **Works with any service** — As long as it exposes OpenAPI (e.g. `/v3/api-docs`)
- **Fast onboarding** — Deploy the discovery agent and get APIs in Winspect

**How it works:** The discovery agent scans Kubernetes services, fetches OpenAPI from well-known endpoints, and creates runtime records. These can be bulk-imported or manually mapped to APIs.

## Coexistence

Both approaches can run in the same cluster:
- Operator-managed APIs (from ApiSpec CRD) are synced directly
- Runtime discovery can still run and create records for services without ApiSpec
- Metadata hash matching prevents duplicates when both apply to the same service

## Recommendation

- **New services / greenfield:** Prefer ApiSpec CRD for GitOps and traceability
- **Legacy / brownfield:** Use runtime discovery for quick wins, migrate to ApiSpec when possible
