// Points every download button at the newest release's APKs.
//   data-download="phone" -> the phone APK (an .apk without "wear" in its name)
//   data-download="watch" -> the Wear OS APK (an .apk with "wear" in its name)
//   data-release="tag" / "phone-size" / "watch-size" -> filled with that text
// Without JavaScript, or if GitHub can't be reached, buttons keep their href:
// the latest release page.
(function () {
  const RELEASES = "https://api.github.com/repos/Dawit-Sh/Bowels/releases/latest";

  function megabytes(bytes) {
    return (bytes / 1048576).toFixed(1) + " MB";
  }

  function fill(name, text) {
    document.querySelectorAll('[data-release="' + name + '"]').forEach(function (el) {
      el.textContent = text;
    });
  }

  fetch(RELEASES, { headers: { Accept: "application/vnd.github+json" } })
    .then(function (response) {
      if (!response.ok) throw new Error("GitHub said " + response.status);
      return response.json();
    })
    .then(function (release) {
      const apks = (release.assets || []).filter(function (a) { return /\.apk$/i.test(a.name); });
      const watch = apks.find(function (a) { return /wear/i.test(a.name); });
      const phone = apks.find(function (a) { return !/wear/i.test(a.name); });

      fill("tag", release.tag_name);
      document.documentElement.dataset.hasWatchApk = watch ? "true" : "false";

      if (phone) {
        fill("phone-size", megabytes(phone.size));
        document.querySelectorAll('[data-download="phone"]').forEach(function (el) {
          el.href = phone.browser_download_url;
          el.removeAttribute("target");
        });
      }
      if (watch) {
        fill("watch-size", megabytes(watch.size));
        fill("watch-file", watch.name);
        document.querySelectorAll('[data-download="watch"]').forEach(function (el) {
          el.href = watch.browser_download_url;
          el.removeAttribute("target");
        });
      }
    })
    .catch(function () {
      // Keep the release-page links.
    });
})();
