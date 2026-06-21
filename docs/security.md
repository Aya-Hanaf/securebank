# Sécurité — SecureBank

## Analyse des dépendances (OWASP Dependency Check)

Le pipeline CI intègre OWASP Dependency Check pour scanner les dépendances Maven
à la recherche de vulnérabilités connues (CVE), avec un seuil d'échec fixé à
CVSS >= 7.0 (vulnérabilités critiques).

### Limitation connue

Ce scan dépend du service externe NVD (National Vulnerability Database,
nvd.nist.gov), qui nécessite une clé API gratuite. Ce service est connu pour
être occasionnellement instable (erreurs 403/503), indépendamment de la
validité de la clé API utilisée — un problème largement documenté dans la
communauté OWASP (voir [dependency-check/DependencyCheck#6834](https://github.com/dependency-check/DependencyCheck/issues/6834)).

En cas d'indisponibilité du service NVD, le job `owasp-check` peut échouer
temporairement sans qu'il s'agisse d'une vulnérabilité réelle détectée dans
le projet. Le retry automatique du pipeline ou une nouvelle tentative
ultérieure résout généralement le problème.

### Configuration

- Plugin : `org.owasp:dependency-check-maven:9.2.0`
- Seuil d'échec : CVSS >= 7.0
- Clé API NVD : stockée comme secret GitHub Actions (`NVD_API_KEY`)