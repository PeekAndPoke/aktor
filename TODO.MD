# General

- [DONE] Move KRAFT to Mono-repo Ultra
- [DONE] KtorFx to Mono-repo Ultra
- [DONE] rename `de.peekandpoke.ktorfx` to `de.peekandpoke.funktor`
- [ ] create reusable FunktorUi (see thebase-web admin)
- [ ] Move Utlra::common.datetime to Ultra::datetime
- [ ] Move Kraft::core.streams to Ultra::streams

- [BAD IDEA] rename all packages to `io.peekandpoke...`

# Funktor

## Auth

- [ ] Send email when password was changed

# Kraft

- [ ] create a builder callback for Kraft.initialize() to configure the behaviour of the managers
- [x] move val Modals: ModalsManager into Kraft.initialize(): Kraft
- [x] move val Popups: PopupsManager into Kraft.initialize(): Kraft
- [x] move val FlashMessage: FlashMessagesManager into Kraft.initialize(): Kraft
- [x] then in the apps main component mount it via Kraft(this: FlowContent)
  - [x] mount the Modals stage
  - [x] mount the Popups stage
  - [x] mount the FlashMessages stage

# Aktor

## Web-App

### SSE-Session

1. create central SSE-connection
2. reconnect automatically

### Storage / State

1. create central chat-storage in app
2. updates from SSE will flow into the central storage
3. Should sending messages also go through this storage?

## More TOOLs or MCPs

### OpenStreetMap

1. Search geo-coordinates for City names etc
2. Reverse geo-search

## Integrate online LLM-Providers

1. Deepseek API
2. Huggingface
3. Google AI
4. Anthropic AI

# Auth-System

## Password Auth

### Forgot password functionality

1. Forgot password link on LoginWidget, when Email-Password-Auth is available
2. Create recovery token
3. Send Email with link to recovery token
4. how do we handle the link with the recovery token in the frontend?
5. What does the recovery token contain? Realm, user, provider...?

## Add providers

### Facebook:

A widely used social media platform with an OAuth API for authentication.

### Twitter:

Provides OAuth for users to access their Twitter accounts and integrate Twitter features.

### LinkedIn:

A professional networking site that offers OAuth for accessing profiles and connections.

### Microsoft:

Offers OAuth through Microsoft account services, allowing access to services like Office 365 and Azure.

### Amazon:

Provides OAuth for accessing Amazon services, particularly useful for ecommerce integrations.

### Apple:

Offers Sign in with Apple, a privacy-focused authentication method.

### Spotify:

OAuth access to user accounts for music and playlist integration.

### Slack:

Provides OAuth for apps that integrate with Slack workspaces.

### Zoom:

Offers OAuth for applications that require video conferencing features.

### Discord:

A popular communication platform also offering OAuth for user authentication.

### Dropbox:

Offers OAuth for applications that require access to user files and folders stored in Dropbox.

### Salesforce:

Provides OAuth for applications that need to access Salesforce data and services.

### Trello:

Offers OAuth for integrations with Trello boards and cards.

### Bitbucket:

Similar to GitHub, provides OAuth for accessing code repositories.

### Shopify:

Offers OAuth for applications that need to interact with Shopify stores.

### Flickr:

Provides OAuth for accessing and managing user photo collections.

### Yelp:

Offers OAuth for accessing business information and reviews.

### Vimeo:

OAuth access for integrations with video content.

### SurveyMonkey:

Provides OAuth for accessing survey data and user accounts.

### Pipedrive:

Offers OAuth for accessing sales pipeline data.

### Okta

A provider for identity and access management, offering OAuth support.

### Auth0

A flexible, drop-in solution to add authentication and authorization services to your applications.

### Azure

ctive Directory: Offers OAuth2 authentication to access resources within Microsoft services.

### Box

A cloud content management and file sharing service with OAuth for file access.

### WordPress

Some WordPress installations and development environments offer OAuth support.

