## org.frc4050.targetgearlift.Main - AKA: Biohazard's Vision Program ##

Hopefully this readme will be updated more in the future...


**How to configure for your platform:**
 1. In your `build.gradle` change the build type to match the platform which you are deploying to: `ext.buildType="windows"` for example.
 2. The `config.biohazard` file also has some settings you may want to adjust. Here you can set the IP of your RoboRIO, the name of the Network Table you will be writing to, and a few other run-time variables.
 3. After configuring the above files to your liking, simply running `gradlew build` will build the vision application and dump everything you need into the `out` folder for use.