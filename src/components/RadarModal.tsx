import React, { useEffect } from 'react';
import { RadarView, RadarViewProps } from './RadarView';

interface RadarModalProps extends RadarViewProps {
  isOpen: boolean;
  onClose: () => void;
}

export const RadarModal: React.FC<RadarModalProps> = ({
  isOpen,
  onClose,
  ...viewProps
}) => {
  // Handle Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="modal-backdrop" onClick={onClose} data-testid="radar-modal-backdrop">
      <div
        className="modal-window radar-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        data-testid="radar-modal"
      >
        {/* Header */}
        <div className="modal-header">
          <div className="radar-title-box">
            <span className="radar-live-badge" data-testid="radar-live-badge">
              LIVE DOPPLER
            </span>
            <h2 className="modal-title" data-testid="radar-modal-title">
              Interactive Weather Radar • {viewProps.locationName}
            </h2>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Close Radar View"
            data-testid="close-radar-modal-btn"
          >
            ✕
          </button>
        </div>

        <RadarView {...viewProps} />
      </div>
    </div>
  );
};
