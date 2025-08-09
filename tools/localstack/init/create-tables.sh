#!/usr/bin/env bash
set -euo pipefail

awslocal dynamodb create-table \
  --table-name PolicyRequests \
  --attribute-definitions \
      AttributeName=policyId,AttributeType=S \
      AttributeName=customerId,AttributeType=S \
  --key-schema \
      AttributeName=policyId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --global-secondary-indexes '[
      {
        "IndexName": "gsi_customer",
        "KeySchema": [{"AttributeName": "customerId", "KeyType": "HASH"}],
        "Projection": {"ProjectionType": "ALL"}
      }
  ]'
