// Headless Droid in a plain Jenkins shell step.
//
// Multibranch Pipeline: on pull request builds Jenkins sets CHANGE_ID (PR number) and
// CHANGE_TARGET (base branch). The stage installs the Droid CLI, then runs the same
// scripts/risk-router.sh that GitHub Actions uses. No plugin, no wrapper: `droid exec`
// is just another command in `sh`.
//
// Credentials expected in Jenkins:
//   factory-api-key   Secret text  -> FACTORY_API_KEY
//   github-token      Secret text  -> GH_TOKEN (repo + pull request write)

pipeline {
  agent any

  options {
    timeout(time: 20, unit: 'MINUTES')
    disableConcurrentBuilds()
  }

  environment {
    FACTORY_API_KEY = credentials('factory-api-key')
    GH_TOKEN        = credentials('github-token')
    GH_REPO         = 'factory-calvin/telecom-autonomy-demo'
    RISK_THRESHOLD_LOW  = '30'
    RISK_THRESHOLD_HIGH = '70'
    RISK_AUTO_MERGE     = 'false'
  }

  stages {
    stage('Install Droid CLI') {
      steps {
        sh '''
          set -euo pipefail
          curl -fsSL https://app.factory.ai/cli | sh
          export PATH="$HOME/.local/bin:$HOME/.factory/bin:$PATH"
          droid --version
        '''
      }
    }

    stage('Droid risk router') {
      when { changeRequest() }
      steps {
        sh '''
          set -euo pipefail
          export PATH="$HOME/.local/bin:$HOME/.factory/bin:$PATH"
          export PR_NUMBER="$CHANGE_ID"
          export BASE_REF="$CHANGE_TARGET"
          ./scripts/risk-router.sh
        '''
      }
    }

    stage('Headless smoke: repo readiness summary') {
      // Read-only droid exec on every build, to show the runtime is just a CLI.
      when { not { changeRequest() } }
      steps {
        sh '''
          set -euo pipefail
          export PATH="$HOME/.local/bin:$HOME/.factory/bin:$PATH"
          mkdir -p risk-router
          droid exec --output-format json \
            "List the validation loops this repo gives an agent (AGENTS.md sections, lint, unit tests per tier, e2e, CI workflows, skills under .factory/skills). Reply in 10 lines or fewer." \
            | tee risk-router/readiness-summary.json
        '''
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'risk-router/*.json, risk-router/*.md', allowEmptyArchive: true
    }
  }
}
