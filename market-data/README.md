# PennyOS market-data companion

Optional data app for this PennyOS fork's Vincit header panel. Install it alongside Home to opt in;
without it the panel is hidden. It has no launcher tile. It is separate from the module-card add-ons
because it supplies a header panel rather than a card.

Home has no `INTERNET` permission. `StockPanelController` reads the companion's cached snapshot on
a worker thread and observes changes only while Home is visible. `StockPanelView` renders native
text and the bundled Vincit vector logo. No WebView, chart engine, continuous animation or new
launcher dependency is involved.

## Data and refresh

`YahooPriceSource` fetches Yahoo Finance's public chart endpoint for `VINCIT.HE` in EUR, with two years
of daily closes. This is an unofficial endpoint without an availability guarantee; the transport and
parser are isolated so a supported feed can replace it. No account, API key or cookies are used.

Home requests an update when shown and every 15 minutes while visible. The companion independently
throttles attempts to 15 minutes, including errors. Android's JobScheduler makes one bounded request
when a network is available; there is no periodic background job, boot receiver or foreground service.
A scheduled request may finish after leaving Home. Queries only read cached data. Errors retain the
last good snapshot. The actual quote timestamp in Helsinki time, including its date, is retained in
the panel's accessibility description; the compact visual panel has only two lines. An unavailable
comparison is an em dash, never an invented zero. Prices may be delayed.

1D compares with the preceding available trading day's close. 1W, 1M and 1Y compare with the last
available close on or before one calendar week, month or year before the quote's date. They are
price changes, excluding cash dividends, using the feed's historical close series. No total-return
or real-time-price claim is made.

## Contract

`content://com.pennyos.marketdata.quotes/quote` returns one `snapshot` JSON column, or no row before
the first successful fetch. Version 1 contains `symbol`, `currency`, `price`, `as_of_ms`,
`fetched_at_ms`, and nullable `change_day`, `change_week`, `change_month`, `change_year` percentages.
`notifyChange` announces a successfully stored update. All data is public market information.
Writes are refused. Only `com.pennyos.home` (or the ADB shell) may call `refresh`; this merely schedules
a throttled job. Home verifies the provider belongs to `com.pennyos.marketdata`.

## Permissions

- `INTERNET`: fetch the one HTTPS Yahoo Finance endpoint.
- `ACCESS_NETWORK_STATE`: allow JobScheduler to wait for connectivity.
- The job service requires the system's `BIND_JOB_SERVICE` permission; apps cannot start it directly.

No telemetry, ads, boot permission or persistent service.

## Build

JDK 17 and Android SDK 34, using the repository's existing Gradle wrapper from the root:

```powershell
$env:ANDROID_HOME = '<Android SDK directory>'
launcher/gradlew.bat -p market-data testReleaseUnitTest lintRelease assembleRelease
```

The APK is `market-data/app/build/outputs/apk/release/app-release.apk`, signed with the same local
debug key as Home. Build from source; do not commit APKs. Installation is an ordinary ADB APK install;
it does not change Home ownership or disable packages.

## Artwork sources

The launcher bundles [Vincit's investor-site logo](https://investors.vincit.com/en/investors/share_information/share_monitor)
as a small Android vector, with a light fill for the night background. The quote portrait is the
[Niilo22.fi thumbs-up cutout](https://niilo22.fi/img/peukku500.png), selected by the TV owner. The fan
site's permission statement applies to that site; no broader redistribution licence is asserted here.
