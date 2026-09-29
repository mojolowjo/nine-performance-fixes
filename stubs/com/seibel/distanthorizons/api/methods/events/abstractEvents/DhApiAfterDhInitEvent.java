// Compile-time stub (see stubs/README.md). The real class is provided at runtime.
package com.seibel.distanthorizons.api.methods.events.abstractEvents;
public abstract class DhApiAfterDhInitEvent
        implements com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEvent<Void>,
                   com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiOneTimeEvent<Void> {
    public DhApiAfterDhInitEvent() {}
    public abstract void afterDistantHorizonsInit(com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam<Void> param);
    public final void fireEvent(com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam<Void> param) { throw new UnsupportedOperationException("stub"); }
}
