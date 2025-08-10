#!/usr/bin/env bash
set -euo pipefail

echo "Creating SQS queue payments-topic ..."
awslocal sqs create-queue --queue-name payments-topic
echo "Creating SQS queue insurance-subscriptions-topic ..."
awslocal sqs create-queue --queue-name insurance-subscriptions-topic