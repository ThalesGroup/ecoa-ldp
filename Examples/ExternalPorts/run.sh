ant all -Ddeploymentname=SENDER
ant all -Ddeploymentname=RECEIVER

(cd 04-Integration/RECEIVER; ./a.out </dev/null) &
(cd 04-Integration/SENDER; ./a.out </dev/null)

killall a.out
