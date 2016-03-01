echo Android Home is: $ANDROID_HOME
echo Waiting for Emulator to Boot.
while true; do
  str=`$ANDROID_HOME/platform-tools/adb shell getprop init.svc.bootanim 2>&1`
  echo -n .
  if [[ $str =~ 'stopped' ]]; then
    break
  fi
  sleep 5
done
echo Finished: $str
echo Emulator has booted.
