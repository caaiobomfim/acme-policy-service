#!/usr/bin/env bash
set -euo pipefail

REQUEST_ID="8d86546c-f580-40a9-ad2c-a6049b908f5b"
PAYMENT_ID="16ba8cb6-4da9-40e2-b71f-e99fd4873a2a"
OCCURRED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

REGION="us-east-1"
ENDPOINT_URL="http://sqs.us-east-1.localhost.localstack.cloud:4566"
QUEUE_NAME="payments-topic"

QUEUE_URL="$(aws sqs get-queue-url \
  --queue-name "$QUEUE_NAME" \
  --region "$REGION" \
  --endpoint-url "$ENDPOINT_URL" \
  --query 'QueueUrl' --output text)"

printf -v MSG '%s' "{
  \"requestId\": \"$REQUEST_ID\",
  \"paymentId\": \"$PAYMENT_ID\",
  \"status\": \"CONFIRMED\",
  \"occurredAt\": \"$OCCURRED_AT\"
}"

aws sqs send-message \
  --queue-url "$QUEUE_URL" \
  --region "$REGION" \
  --endpoint-url "$ENDPOINT_URL" \
  --message-body "$MSG" \
  --message-attributes '{
    "contentType":{"DataType":"String","StringValue":"application/json"},
    "JavaType":{"DataType":"String","StringValue":"com.acme.insurance.policy.app.dto.integration.PaymentResultEvent"}
  }'
