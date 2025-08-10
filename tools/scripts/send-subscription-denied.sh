#!/usr/bin/env bash
set -euo pipefail

REQUEST_ID="8528d908-b609-419a-97c6-e4ff99f8158c"
OCCURRED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

REGION="us-east-1"
ENDPOINT_URL="http://sqs.us-east-1.localhost.localstack.cloud:4566"
QUEUE_NAME="insurance-subscriptions-topic"

QUEUE_URL="$(aws sqs get-queue-url \
  --queue-name "$QUEUE_NAME" \
  --region "$REGION" \
  --endpoint-url "$ENDPOINT_URL" \
  --query 'QueueUrl' --output text)"

printf -v MSG '%s' "{
  \"requestId\": \"$REQUEST_ID\",
  \"status\": \"DENIED\",
  \"occurredAt\": \"$OCCURRED_AT\"
}"

aws sqs send-message \
  --queue-url "$QUEUE_URL" \
  --region "$REGION" \
  --endpoint-url "$ENDPOINT_URL" \
  --message-body "$MSG" \
  --message-attributes '{
    "contentType":{"DataType":"String","StringValue":"application/json"},
    "JavaType":{"DataType":"String","StringValue":"com.acme.insurance.policy.app.dto.integration.SubscriptionResultEvent"}
  }'
