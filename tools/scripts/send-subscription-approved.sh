#!/usr/bin/env bash
set -euo pipefail

REQUEST_ID="45dc4a1f-69c8-44bb-9c43-39e121a4c6c1"
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
  \"status\": \"AUTHORIZED\",
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
