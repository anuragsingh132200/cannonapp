import React, { useEffect, forwardRef, useImperativeHandle, useRef } from 'react';
import { requireNativeComponent, ViewProps, Platform, View, Text, StyleSheet, UIManager, findNodeHandle } from 'react-native';
import { useCameraPermissions } from 'expo-camera';

interface FaceMeshCameraViewProps extends ViewProps {
    onVideoRecorded?: (event: { nativeEvent: { uri: string } }) => void;
}

const NativeFaceMeshCameraView = requireNativeComponent<FaceMeshCameraViewProps>('FaceMeshCameraView');

export const FaceMeshCameraView = forwardRef<any, FaceMeshCameraViewProps>((props, ref) => {
    const nativeRef = useRef<any>(null);
    const [permission, requestPermission] = useCameraPermissions();
    const resolveRecordingRef = useRef<((uri: string) => void) | null>(null);

    useImperativeHandle(ref, () => ({
        recordAsync: () => {
             return new Promise<{uri: string}>((resolve) => {
                 resolveRecordingRef.current = (uri: string) => resolve({ uri });
                 const handle = findNodeHandle(nativeRef.current);
                 if (handle) {
                     UIManager.dispatchViewManagerCommand(
                         handle,
                         UIManager.getViewManagerConfig('FaceMeshCameraView').Commands.startRecording.toString(),
                         []
                     );
                 }
             });
        },
        stopRecording: () => {
             const handle = findNodeHandle(nativeRef.current);
             if (handle) {
                 UIManager.dispatchViewManagerCommand(
                     handle,
                     UIManager.getViewManagerConfig('FaceMeshCameraView').Commands.stopRecording.toString(),
                     []
                 );
             }
        }
    }));

    const handleVideoRecorded = (event: any) => {
        if (resolveRecordingRef.current) {
            resolveRecordingRef.current(event.nativeEvent.uri);
            resolveRecordingRef.current = null;
        }
        if (props.onVideoRecorded) props.onVideoRecorded(event);
    };
    useEffect(() => {
        if (Platform.OS === 'android' && permission && !permission.granted && permission.canAskAgain) {
            requestPermission();
        }
    }, [permission, requestPermission]);

    if (!permission) {
        return <View style={styles.container} />;
    }

    if (!permission.granted) {
        return (
            <View style={styles.container}>
                <Text style={styles.text}>We need your permission to show the camera</Text>
            </View>
        );
    }

    if (Platform.OS !== 'android') {
        return (
            <View style={styles.container}>
                <Text style={styles.text}>FaceMeshCameraView is only supported on Android.</Text>
            </View>
        );
    }

    return (
        <NativeFaceMeshCameraView 
            {...props} 
            ref={nativeRef} 
            onVideoRecorded={handleVideoRecorded} 
            style={[styles.camera, props.style]} 
        />
    );
});

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#000',
    },
    text: {
        color: '#fff',
        textAlign: 'center',
    },
    camera: {
        flex: 1,
    },
});
