## needsLocalTime

If true, the component has access to the 'local' time reference.


## needsSystemTime

If true, the component has access to the 'system' time reference.


## needsUTCTime

If true, the component has access to the 'UTC' time reference.


## needsTimeResolution

If true, the component has access to the resolution of the time references it uses.


## hasReset

If true, the component must define a 'reset' lifecycle entrypoint.


## autostartExternalThread

Applicable for EXTERNAL components.
If true, the external thread is started automatically, before component init.
If false, the external thread must be started by the component code using the container API.


## hasWarmStartContext

If true, allows a component to save its warm start (non-volatile) context such that it will 
be restored by the infrastructure upon a warm restart.
