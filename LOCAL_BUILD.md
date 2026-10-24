# Ariana Mobile - Local Build

این نسخه برای حالت **Local Files** آماده شده است.

## رفتار نسخه جدید

- `distribution.json` از اینترنت دریافت نمی‌شود.
- فایل‌های GTA/SA-MP دانلود نمی‌شوند.
- CDN بازی حذف شده است.
- News / Donate API هنگام Startup صدا زده نمی‌شوند.
- سرور داخل لانچر روی `100.78.167.13:7777` تنظیم شده است.
- GitHub Actions فایل APK Release را می‌سازد.

## محل فایل‌های بازی روی گوشی

بعد از نصب APK، فایل‌های آماده بازی را دستی در این مسیر قرار بده:

`/storage/emulated/0/Android/data/com.touch.mobile.dark/files/`

ساختار پوشه‌های فایل بازی را تغییر نده.

> نکته: خود فایل‌های GTA/SA-MP را داخل GitHub قرار نده. پوشه `local-game-files` فقط برای راهنماست.

## Build با GitHub

1. کل پروژه را در یک Repository جدید GitHub قرار بده.
2. به تب **Actions** برو.
3. Workflow با نام **Build Android APK** را انتخاب کن.
4. روی **Run workflow** بزن.
5. بعد از اتمام Build، از بخش Artifacts فایل `ariana-mobile-release` را دریافت کن.

## تغییر IP/Port

فقط این مقدار را در فایل زیر تغییر بده:

`src/config/localDistribution.ts`

```ts
export const LOCAL_SERVER_ADDRESS = '100.78.167.13:7777';
```

## نکته مهم درباره اتصال بازی

لانچر دیگر مرحله Download/Update فایل‌های بازی را اجرا نمی‌کند. با این حال، اتصال نهایی SA-MP توسط Native client (`libsamp.so`) انجام می‌شود. اگر نسخه Native بازی سرور را از یک تنظیم داخلی خاص بخواند، ممکن است لازم باشد آن قسمت Native نیز تنظیم شود؛ این موضوع جدا از سیستم Download لانچر است.
