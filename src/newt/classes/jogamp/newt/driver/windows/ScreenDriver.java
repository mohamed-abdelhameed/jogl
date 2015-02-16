/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */
package jogamp.newt.driver.windows;

import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.util.Rectangle;

import jogamp.newt.MonitorModeProps;
import jogamp.newt.ScreenImpl;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.Screen;

public class ScreenDriver extends ScreenImpl {

    static {
        DisplayDriver.initSingleton();
        if( Screen.DEBUG ) {
            dumpMonitorInfo0();
        }
    }

    public ScreenDriver() {
    }

    @Override
    protected void createNativeImpl() {
        aScreen = new DefaultGraphicsScreen(getDisplay().getGraphicsDevice(), screen_idx);
    }

    @Override
    protected void closeNativeImpl() {
    }

    private final String getAdapterName(final int adapter_idx) {
        return getAdapterName0(adapter_idx);
    }
    private final String getMonitorName(final String adapterName, final int monitor_idx, final boolean onlyActive) {
        return getMonitorName0(adapterName, monitor_idx, onlyActive);
    }
    private final int getFirstActiveMonitor(final String adapterName) {
        return getFirstActiveMonitor0(adapterName);
    }

    private final MonitorMode getMonitorModeImpl(final MonitorModeProps.Cache cache, final String adapterName, final int mode_idx) {
        if( null == adapterName ) {
            return null;
        }
        final int[] modeProps = getMonitorMode0(adapterName, mode_idx);
        if ( null == modeProps || 0 >= modeProps.length) {
            return null;
        }
        return MonitorModeProps.streamInMonitorMode(null, cache, modeProps, 0);
    }

    @Override
    protected void collectNativeMonitorModesAndDevicesImpl(final MonitorModeProps.Cache cache) {
        ArrayHashSet<MonitorMode> supportedModes = new ArrayHashSet<MonitorMode>();
        int adapter_idx;
        String adapterName;
        for(adapter_idx=0; null != ( adapterName = getAdapterName(adapter_idx) ); adapter_idx++ ) {
            final int activeMonitorIdx = getFirstActiveMonitor(adapterName);
            if( 0 <= activeMonitorIdx ) {
                int mode_idx = 0;
                MonitorMode mode;
                do {
                    mode = getMonitorModeImpl(cache, adapterName, mode_idx);
                    if( null != mode ) {
                        supportedModes.getOrAdd(mode);
                        // next mode on same monitor
                        mode_idx++;
                    }
                } while( null != mode);
                if( 0 < mode_idx ) {
                    // has at least one mode -> add device
                    final MonitorMode currentMode = getMonitorModeImpl(cache, adapterName, -1);
                    if ( null != currentMode ) { // enabled
                        final int[] monitorProps = getMonitorDevice0(adapterName, adapter_idx);
                        // merge monitor-props + supported modes
                        MonitorModeProps.streamInMonitorDevice(cache, this, currentMode, null, supportedModes, monitorProps, 0, null);

                        // next monitor, 1st mode
                        supportedModes = new ArrayHashSet<MonitorMode>();
                    }
                }
            }
        }
    }

    @Override
    protected boolean updateNativeMonitorDeviceViewportImpl(final MonitorDevice monitor, final float[] pixelScale, final Rectangle viewportPU, final Rectangle viewportWU) {
        final String adapterName = getAdapterName(monitor.getId());
        if( null != adapterName ) {
            final int activeMonitorIdx = getFirstActiveMonitor(adapterName);
            if( 0 <= activeMonitorIdx ) {
                final int[] monitorProps = getMonitorDevice0(adapterName, monitor.getId());
                int offset = MonitorModeProps.IDX_MONITOR_DEVICE_VIEWPORT;
                viewportPU.set(monitorProps[offset++], monitorProps[offset++], monitorProps[offset++], monitorProps[offset++]);
                viewportWU.set(monitorProps[offset++], monitorProps[offset++], monitorProps[offset++], monitorProps[offset++]);
                return true;
            }
        }
        return false;
    }

    @Override
    protected MonitorMode queryCurrentMonitorModeImpl(final MonitorDevice monitor) {
        return getMonitorModeImpl(null, getAdapterName(monitor.getId()), -1);
    }

    @Override
    protected boolean setCurrentMonitorModeImpl(final MonitorDevice monitor, final MonitorMode mode)  {
        return setMonitorMode0(monitor.getId(),
                               -1, -1, // no fixed position!
                               mode.getSurfaceSize().getResolution().getWidth(),
                               mode.getSurfaceSize().getResolution().getHeight(),
                               mode.getSurfaceSize().getBitsPerPixel(),
                               (int)mode.getRefreshRate(), // simply cut-off, orig is int
                               mode.getFlags(),
                               mode.getRotation());
    }

    @Override
    protected int validateScreenIndex(final int idx) {
        return 0; // big-desktop w/ multiple monitor attached, only one screen available
    }

    @Override
    protected void calcVirtualScreenOriginAndSize(final Rectangle viewport, final Rectangle viewportInWindowUnits) {
        viewport.set(getVirtualOriginX0(), getVirtualOriginY0(), getVirtualWidthImpl0(), getVirtualHeightImpl0());
        viewportInWindowUnits.set(viewport);
    }

    // Native calls
    private native int getVirtualOriginX0();
    private native int getVirtualOriginY0();
    private native int getVirtualWidthImpl0();
    private native int getVirtualHeightImpl0();

    private static native void dumpMonitorInfo0();
    private native String getAdapterName0(int adapter_idx);
    private native String getMonitorName0(String adapterName, int monitor_idx, boolean onlyActive);
    private native int getFirstActiveMonitor0(String adapterName);
    private native int[] getMonitorMode0(String adapterName, int mode_idx);
    private native int[] getMonitorDevice0(String adapterName, int adapter_idx);
    private native boolean setMonitorMode0(int adapter_idx, int x, int y, int width, int height, int bits, int freq, int flags, int rot);
}
