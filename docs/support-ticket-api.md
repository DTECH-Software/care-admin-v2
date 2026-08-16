# Support Ticket API

All endpoints use `POST` and are available under the Admin context path `/admin/api/v1/support-tickets`.

The WeCare Admin page uses `/wecare-admin/...`; the WeCare App page uses `/wecare-app/...`. Payloads are identical. The page in the URL fixes the ticket system type, so clients cannot submit or change `systemType`.

## Endpoints

| Action | WeCare Admin | WeCare App | Required task |
|---|---|---|---|
| Reference data | `/wecare-admin/reference-data` | `/wecare-app/reference-data` | `REF_DATA` |
| Filter | `/wecare-admin/filter` | `/wecare-app/filter` | `SEARCH` |
| Create | `/wecare-admin/add` | `/wecare-app/add` | `ADD` |
| View | `/wecare-admin/view` | `/wecare-app/view` | `VIEW` |
| Reply | `/wecare-admin/reply` | `/wecare-app/reply` | `ADD` |
| Status update | `/wecare-admin/status/update` | `/wecare-app/status/update` | `UPDATE` |

Attachments additionally require `FILE_UPLOAD`. No developer-assignment endpoint or field exists.

## Common channel fields

Every request contains:

```json
{
  "username": "admin.user",
  "ip": "192.168.1.10",
  "userAgent": "Chrome",
  "message": "REF_DATA"
}
```

Use `REF_DATA`, `FILTER_LIST`, `ADD`, `VIEW`, or `UPDATE` according to the action.

## Reference data

```json
{
  "username": "admin.user",
  "ip": "192.168.1.10",
  "userAgent": "Chrome",
  "message": "REF_DATA"
}
```

The response includes privileges, companies assigned to the logged-in user, page-specific categories, priorities, and statuses.

## Filter

```json
{
  "username": "admin.user",
  "ip": "192.168.1.10",
  "userAgent": "Chrome",
  "message": "FILTER_LIST",
  "page": 0,
  "size": 10,
  "sortColumn": "lastModifiedDate",
  "sortDirection": "DESC",
  "search": {
    "ticketNo": "ADM-",
    "companyCode": "SGCS",
    "category": "CLAIM_APPROVAL",
    "priority": "HIGH",
    "status": "OPEN",
    "subject": "approval",
    "createdBy": "admin.user",
    "fromDate": "2026-08-01",
    "toDate": "2026-08-31"
  }
}
```

## Create

```json
{
  "username": "admin.user",
  "ip": "192.168.1.10",
  "userAgent": "Chrome",
  "message": "ADD",
  "companyCode": "SGCS",
  "category": "CLAIM_APPROVAL",
  "subject": "Claim approval screen is not loading",
  "description": "The issue started after selecting the policy year.",
  "priority": "HIGH",
  "attachments": [
    {
      "fileName": "error.png",
      "fileType": "image/png",
      "file": "BASE64_CONTENT"
    }
  ]
}
```

Maximum five attachments per request and 10 MB per attachment. Supported formats are PNG, JPEG/JPG, and PDF. Attachments reuse the configured Admin document storage.

## View

```json
{
  "username": "admin.user",
  "ip": "192.168.1.10",
  "userAgent": "Chrome",
  "message": "VIEW",
  "id": 1
}
```

The response includes ticket details, Base64 attachments, replies, and status history.

## Reply

```json
{
  "username": "admin.user",
  "ip": "192.168.1.10",
  "userAgent": "Chrome",
  "message": "ADD",
  "id": 1,
  "reply": "Please confirm whether this happens for every policy year.",
  "attachments": []
}
```

## Status update

```json
{
  "username": "support.user",
  "ip": "192.168.1.20",
  "userAgent": "Chrome",
  "message": "UPDATE",
  "id": 1,
  "status": "RESOLVED",
  "remark": "Fix deployed to UAT",
  "resolution": "Corrected the policy-year lookup."
}
```

Allowed flow:

`OPEN -> IN_PROGRESS/RESOLVED/CLOSED`

`IN_PROGRESS -> WAITING_FOR_CLIENT/RESOLVED/CLOSED`

`WAITING_FOR_CLIENT -> IN_PROGRESS/RESOLVED/CLOSED`

`RESOLVED -> REOPENED/CLOSED`

`CLOSED -> REOPENED`

`REOPENED -> IN_PROGRESS/WAITING_FOR_CLIENT/RESOLVED/CLOSED`

A resolution is required when resolving or closing a ticket. Reopening clears the previous resolution and completion dates.

## Company access

Users can create, search, view, reply to, and update only tickets belonging to companies assigned to their Admin account. Assign all supported companies to the central support role/accounts that need system-wide visibility.
