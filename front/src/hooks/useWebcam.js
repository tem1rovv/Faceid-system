import { useRef, useState, useCallback, useEffect } from 'react';

/**
 * useWebcam — manages webcam stream lifecycle and frame capture.
 *
 * Returns:
 *   videoRef  — attach to <video> element
 *   active    — whether stream is live
 *   error     — camera error string or null
 *   start()   — request camera permission and start stream
 *   stop()    — stop all tracks and release camera
 *   capture() — snapshot the current frame as a base64 JPEG data URL
 */
export function useWebcam() {
  const videoRef  = useRef(null);
  const streamRef = useRef(null);
  const [active, setActive] = useState(false);
  const [error,  setError]  = useState(null);

  const start = useCallback(async () => {
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          width:      { ideal: 1280 },
          height:     { ideal: 720 },
          facingMode: 'user',
        },
        audio: false,
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setActive(true);
    } catch (err) {
      console.error('Camera error:', err);
      setError('Camera access denied. Please allow camera permissions and reload.');
    }
  }, []);

  const stop = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(t => t.stop());
      streamRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setActive(false);
  }, []);

  /**
   * Capture current video frame as a JPEG base64 data URL.
   * Returns null if the video is not active.
   */
  const capture = useCallback((quality = 0.92) => {
    const video = videoRef.current;
    if (!video || !active) return null;

    const w = video.videoWidth  || 640;
    const h = video.videoHeight || 480;

    const canvas = document.createElement('canvas');
    canvas.width  = w;
    canvas.height = h;

    const ctx = canvas.getContext('2d');
    // Mirror horizontally to undo CSS mirror transform
    ctx.translate(w, 0);
    ctx.scale(-1, 1);
    ctx.drawImage(video, 0, 0, w, h);

    return canvas.toDataURL('image/jpeg', quality);
  }, [active]);

  // Cleanup on unmount
  useEffect(() => () => stop(), [stop]);

  return { videoRef, active, error, start, stop, capture };
}
