COMMIT_MESSAGE=$1
BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ -z "$COMMIT_MESSAGE" ]; then
  echo "⚠️ Commit message is not set. Exiting..."
  exit 1
fi
if [ ! -z "$2" ]; then
  BRANCH=$2
fi

echo "🔄 Committing changes to $BRANCH with user $GITHUB_ACTOR"

if ! git diff-index --quiet HEAD --; then
  git config --local user.name "$GITHUB_ACTOR"
  git config --local user.email "${GITHUB_ACTOR}@users.noreply.github.com"
  git commit -am "$COMMIT_MESSAGE"
  git push origin $BRANCH
else
  echo "⚠️ No changes to commit"
fi
