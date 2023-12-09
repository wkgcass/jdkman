# jdkman

## What is `jdkman`

`jdkman` is a command line tool which helps you manage multiple JDK instances.

[An asciinema cast](https://asciinema.org/a/625934) shows how to use `jdkman`.  
[This video](https://www.bilibili.com/video/BV1Gu4y137cg/) shows how to configure `jdkman` on Windows.

## How to install

### 1. Download

Download the pre-built binary from the [release page](https://github.com/wkgcass/jdkman/releases).  
You will need to download the binary corresponding to your operating system and cpu architecture.

Rename the downloaded file `jdkman-$os-$arch` to `jdkman`  
You will also need to run `chmod +x jdkman` on Linux or macOS.

### 2. Configure

#### bash or zsh

Add the following lines in your `~/.bashrc` or `~/.bash_profile` or `~/.zshrc`

```shell
## add jdkman to your PATH environment variable.
export PATH="$PATH:/path/to/jdkman"
eval "`jdkman init sh`"
```

#### Powershell

Add `\path\to\jdkman` to your PATH environment variable.

Then configure the profile script by entering the following lines in powershell:

```powershell
echo $profile ## show the profile script location
New-Item -ItemType Directory -Path (Split-Path $profile) -Force
'Invoke-Expression -Command (jdkman init pwsh)' | Out-File -FilePath $profile -Append
```

#### Windows cmd

Since cmd doesn't support `eval` nor `source`, there's very little that `jdkman` could provide.

Add `\path\to\jdkman` to your PATH environment variable.  
Add `\YourHomePath\jdkman-scripts` to your PATH environment variable.

Then in cmd, run once (for each time you upgrades `jdkman`):

```cmd
jdkman init
```

> You can ignore the init output.

## How to use

### 1. Add JDKs

```shell
jdkman add /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
```

### 2. Show JDKs

```shell
jdkman list
```

### 3. Set JDK version for each project

Add a file `.java-version` in the root directory of your project with the following content:

```
21
```

`21` could be changed to any JDK versions, e.g. `11`, `11.0.2`, `1.8`.  
`jdkman` will find the most appropriate JDK version for you.

You could also specify the `implementor` (or `vendor`) of the JDK:  
Specify the vendor name, add a colon, then specify the jdk version:

```
Oracle Corporation:21.0.1+12-29
```

### 4. Use java

```shell
java -version
echo $JAVA_HOME
```

### 5. The `cd` command and `JAVA_HOME`

For `bash|zsh|powershell`, the `cd` command is replaced with a convenient function: `cdjh`  
it will `cd` into the specified directory, and configure `JAVA_HOME` environment variable automatically.

You could also instead explicitly call the `cdjh` function.

Note: This feature is not provided for `cmd`, you have to set it manually by executing the following command:  
```cmd
for /f "delims=" %%i in ('jdkman which') do set "JAVA_HOME=%%i"
```

## How to build

### 1. Prerequisites

* JDK 21
* GraalVM Native Image for JDK 21

To build `jdkman-proxy`, you will need `rust|cargo`.  
It's already built and placed in resource folder, so you can skip `rust`.

### 2. Build

#### Linux or macOS

```shell
./gradlew clean shadowJar
native-image \
	--enable-preview \
	-jar build/libs/jdkman.jar \
	-Ob -march=compatibility \
	--no-fallback \
	-o jdkman
```

#### Windows (using powershell)

```powershell
# build `jdkman-proxy`
$arch="x86_64" # change to aarch64 if running arm windows
cd .\src\main\rust\
cargo build --release
Copy-Item .\target\release\jdkman-proxy.exe ..\resources\io\vproxy\jdkman\res\jdkman_proxy-windows-$arch.exe
cd ..\..\..\

# build jdkman
.\gradlew.bat clean shadowJar
native-image `
	--enable-preview `
	-jar build\libs\jdkman.jar `
	--features=io.vproxy.jdkman.res.Feature `
	--static `
	-Ob -march=compatibility `
	--no-fallback `
	-o jdkman
```

#### Linux static image in docker

You can build a linux static native image using `vproxyio/graalvm-jdk-21`

```shell
docker run --name jdkman-build -it -v `pwd`:/workdir vproxyio/graalvm-jdk-21 /bin/bash
## inside docker
./gradlew clean shadowJar
LIBC="musl" ## or glibc
native-image \
	--enable-preview \
	-jar build/libs/jdkman.jar \
	--static --libc=$LIBC \
	-Ob -march=compatibility \
	--no-fallback \
	-o jdkman
```
