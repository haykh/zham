{ pkgs, lib, config, ... }:

let
  # Pin EXACTLY the SDK pieces we use. Change a version here and nothing else
  # gets downloaded. `nix store gc` removes all of it.
  platformVersion = "35";
  buildToolsVersion = "35.0.0";
in
{
  # --- Android toolchain ------------------------------------------------------
  # The `android` module wraps androidenv. It composes only the pinned SDK
  # pieces below and — crucially on NixOS — exports GRADLE_OPTS with the
  # `aapt2FromMavenOverride` pointing at the SDK's patched aapt2, plus
  # ANDROID_HOME. So none of that has to be done by hand anymore.
  android = {
    enable = true;
    platforms.version = [ platformVersion ];
    buildTools.version = [ buildToolsVersion ];
    abis = [ "x86_64" ];
    systemImageTypes = [ "google_apis" ];
    emulator.enable = true;
    systemImages.enable = true;
    sources.enable = false;
    ndk.enable = false;
  };

  # --- JDK + Gradle -----------------------------------------------------------
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk17;
    gradle.enable = true;
  };

  # --- Editor tooling (nvim / VS Code) ---------------------------------------
  # Mason is disabled on NixOS, so these must be on PATH — here they are, but
  # only inside this environment.
  packages = [
    pkgs.kotlin-language-server # LSP for .kt / .kts
    pkgs.ktlint                 # Kotlin formatter + linter
    pkgs.lemminx                # XML LSP (AndroidManifest.xml, res/*.xml)
    pkgs.nixfmt-rfc-style       # `nixfmt` binary used by conform
  ];

  # --- One-shot helpers, kept out of your global PATH ------------------------
  scripts.create-avd.exec = ''
    set -euo pipefail
    # androidenv installs cmdline-tools under a versioned dir, not 'latest'.
    avdmanager="$(echo "$ANDROID_HOME"/cmdline-tools/*/bin/avdmanager)"
    echo "no" | "$avdmanager" create avd \
      --force \
      --name worldclock \
      --package 'system-images;android-${platformVersion};google_apis;x86_64' \
      --device "pixel_6"

    # Match the Nothing Phone (2a): 1084x2412 display (its ~394 ppi rounds to the
    # same 420dpi bucket as the pixel_6 base). Drop the device frame so the custom
    # resolution renders without the mismatched Pixel bezel.
    cfg="$ANDROID_AVD_HOME/worldclock.avd/config.ini"
    sed -i \
      -e 's/^hw\.lcd\.width = .*/hw.lcd.width = 1084/' \
      -e 's/^hw\.lcd\.height = .*/hw.lcd.height = 2412/' \
      -e 's/^showDeviceFrame = .*/showDeviceFrame = no/' \
      "$cfg"
    echo "AVD 'worldclock' created (Nothing Phone 2a display) in $ANDROID_AVD_HOME"
  '';

  scripts.run-emulator.exec = ''
    # The SDK wrapper puts build-tools/lib64 on LD_LIBRARY_PATH, whose older
    # libc++ lacks symbols the emulator's bundled abseil needs (causing a
    # "libabseil_dll.so: undefined symbol ...basic_ostringstream..." crash).
    # Prepend the emulator's own lib64 so its matching libc++ wins.
    export LD_LIBRARY_PATH="$ANDROID_HOME/emulator/lib64:''${LD_LIBRARY_PATH:-}"
    # -gpu swiftshader_indirect is the safe default on NixOS; drop it or use
    # '-gpu host' once you confirm your GPU drivers work for a speed boost.
    exec "$ANDROID_HOME"/emulator/emulator -avd worldclock -gpu swiftshader_indirect "$@"
  '';

  # "Hot reload" for the CLI: rebuild + reinstall-in-place + relaunch on every
  # save. Not stateful hot reload (that needs Android Studio), but no manual
  # uninstall and the app is back on screen in a few seconds.
  scripts.watch-app.exec = ''
    set -euo pipefail
    exec ${pkgs.watchexec}/bin/watchexec \
      --restart \
      --exts kt,kts,xml \
      --watch app/src \
      --watch app/build.gradle.kts \
      -- './gradlew installDebug && adb shell am start -n io.github.haykh.zham/.MainActivity'
  '';

  enterShell = ''
    # --- Everything heavy & mutable lives inside this directory ---
    export GRADLE_USER_HOME="$PWD/.cache/gradle"
    export ANDROID_USER_HOME="$PWD/.cache/android"
    export ANDROID_AVD_HOME="$PWD/.cache/android/avd"
    export ANDROID_EMULATOR_HOME="$PWD/.cache/android"
    mkdir -p "$ANDROID_AVD_HOME"

    # Put adb (and other platform-tools) on PATH for the watch loop / manual use.
    export PATH="$ANDROID_HOME/platform-tools:$PATH"

    cat <<EOF
    World Clock dev shell ready (editor-agnostic; use nvim or VS Code).
      SDK:  $ANDROID_HOME  (in /nix/store, shared, removed via 'nix store gc')
      Data: ./.cache       (AVDs + gradle caches, removed via 'rm -rf .cache')

    First time:
      gradle wrapper --gradle-version 8.10.2   # generate ./gradlew (once)
      create-avd                               # build the emulator image into ./.cache
    Each session:
      run-emulator &   # boot the emulator (wait for the home screen)
      watch-app        # rebuild + reinstall + relaunch on every save ("hot reload")
    EOF
  '';
}
