# Authentication Guide

This document describes how to authenticate with the GO-Commerce API using OAuth2/Keycloak.

## Authentication Endpoints

The following endpoints are available for authentication:

### 1. Login

**Endpoint:** `POST /api/auth/login`

**Description:** Authenticates a user and returns access and refresh tokens.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response:**
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "roles": ["string"]
}
```

**Possible Status Codes:**
- 200 OK - Authentication successful
- 400 Bad Request - Invalid request format
- 401 Unauthorized - Invalid credentials

### 2. Refresh Token

**Endpoint:** `POST /api/auth/refresh`

**Description:** Refreshes an expired access token using a valid refresh token.

**Request Body:**
```json
{
  "refreshToken": "string"
}
```

**Response:**
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "roles": ["string"]
}
```

**Possible Status Codes:**
- 200 OK - Token refresh successful
- 400 Bad Request - Invalid request format
- 401 Unauthorized - Invalid or expired refresh token

### 3. Logout

**Endpoint:** `DELETE /api/auth/logout?refreshToken={refreshToken}`

**Description:** Invalidates a refresh token, effectively logging out the user.

**Query Parameters:**
- refreshToken: The refresh token to invalidate

**Response:**
```json
{
  "message": "Successfully logged out"
}
```

**Possible Status Codes:**
- 200 OK - Logout successful
- 400 Bad Request - Missing refresh token
- 500 Internal Server Error - Logout failed

### 4. Validate Token

**Endpoint:** `GET /api/auth/validate?token={token}`

**Description:** Validates an access token.

**Query Parameters:**
- token: The access token to validate

**Response:**
```json
{
  "message": "Token is valid"
}
```

**Possible Status Codes:**
- 200 OK - Token is valid
- 400 Bad Request - Missing token
- 401 Unauthorized - Token is invalid or expired

## Using Authentication Tokens

1. **Include the access token in the Authorization header for protected API requests:**

```
Authorization: Bearer {accessToken}
```

2. **When the access token expires, use the refresh token to obtain a new access token.**

3. **Store tokens securely on the client side. Never store them in localStorage or cookies without proper security measures.**

## Roles and Permissions

The GO-Commerce platform uses the following roles:

1. **admin** - Platform administrators with full access to all functionality
2. **tenant-admin** - Tenant administrators with access to their tenant's resources only
3. **user** - Regular users with restricted access

The roles are included in the token response and can be used for client-side authorization logic.

## Error Handling

Authentication errors follow this format:

```json
{
  "error": "Error description"
}
```

Common error messages include:
- "Authentication failed: Invalid username or password"
- "Token refresh failed: Invalid or expired refresh token"
- "Token is invalid or expired"

## Security Considerations

1. Always use HTTPS for all authentication requests
2. Implement proper token storage on client-side
3. Use short-lived access tokens
4. Implement automatic token refresh logic in clients
5. Properly handle authentication errors