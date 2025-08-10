#!/usr/bin/env bash
set -euo pipefail

echo "Creating SQS queue orders-topic ..."
awslocal sqs create-queue --queue-name orders-topic