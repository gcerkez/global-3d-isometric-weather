import '@testing-library/jest-dom';

// Polyfill HTMLCanvasElement.getContext for jsdom testing
if (typeof window !== 'undefined') {
  HTMLCanvasElement.prototype.getContext = function (contextType: string) {
    if (contextType === '2d') {
      return {
        clearRect: () => {},
        fillRect: () => {},
        beginPath: () => {},
        moveTo: () => {},
        lineTo: () => {},
        stroke: () => {},
        fill: () => {},
        arc: () => {},
        ellipse: () => {},
        drawImage: () => {},
        save: () => {},
        restore: () => {},
        translate: () => {},
        scale: () => {},
        rotate: () => {},
        createLinearGradient: () => ({ addColorStop: () => {} }),
        createRadialGradient: () => ({ addColorStop: () => {} }),
        fillText: () => {},
        measureText: () => ({ width: 0 }),
        roundRect: () => {},
        quadraticCurveTo: () => {},
        closePath: () => {},
      } as unknown as RenderingContext;
    }
    return null;
  } as unknown as typeof HTMLCanvasElement.prototype.getContext;

  HTMLCanvasElement.prototype.getBoundingClientRect = () => ({
    width: 600,
    height: 300,
    top: 0,
    left: 0,
    right: 600,
    bottom: 300,
    x: 0,
    y: 0,
    toJSON: () => {},
  });
}
