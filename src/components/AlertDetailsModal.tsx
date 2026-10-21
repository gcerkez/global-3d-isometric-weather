import React, { useEffect } from 'react';
import { WeatherAlert } from '../types/weather';

interface AlertDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  alert: WeatherAlert | null;
}

export const AlertDetailsModal: React.FC<AlertDetailsModalProps> = ({
  isOpen,
  onClose,
  alert,
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !alert) return null;

  const severityColor =
    alert.severity === 'Extreme' ? '#ef4444' :
    alert.severity === 'Severe' ? '#f97316' :
    alert.severity === 'Moderate' ? '#facc15' : '#38bdf8';

  const formatIsoTime = (isoStr?: string) => {
    if (!isoStr) return null;
    try {
      const d = new Date(isoStr);
      return d.toLocaleString([], {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
      });
    } catch {
      return isoStr;
    }
  };

  const onsetStr = formatIsoTime(alert.onset);
  const expiresStr = formatIsoTime(alert.expires);

  return (
    <div
      className="modal-backdrop"
      onClick={onClose}
      data-testid="alert-modal-backdrop"
    >
      <div
        className="modal-window alert-details-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        data-testid="alert-details-modal"
      >
        <div className="modal-header">
          <div className="alert-header-title-box">
            <span
              className="alert-severity-badge"
              style={{ backgroundColor: `${severityColor}26`, color: severityColor, borderColor: severityColor }}
              data-testid="alert-severity-badge"
            >
              {alert.severity.toUpperCase()}
            </span>
            {alert.urgency && (
              <span className="alert-urgency-badge">
                {alert.urgency.toUpperCase()}
              </span>
            )}
            <h2 className="modal-title" data-testid="alert-modal-title">
              {alert.event}
            </h2>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Close Alert Details"
            data-testid="close-alert-modal-btn"
          >
            ✕
          </button>
        </div>

        <div className="alert-details-body">
          {/* Metadata Timing Row */}
          <div className="alert-timing-card">
            {onsetStr && (
              <div className="timing-item">
                <span className="timing-label">EFFECTIVE:</span>
                <span className="timing-val">{onsetStr}</span>
              </div>
            )}
            {expiresStr && (
              <div className="timing-item">
                <span className="timing-label">EXPIRES:</span>
                <span className="timing-val">{expiresStr}</span>
              </div>
            )}
            {alert.senderName && (
              <div className="timing-item">
                <span className="timing-label">ISSUED BY:</span>
                <span className="timing-val">{alert.senderName}</span>
              </div>
            )}
          </div>

          {/* Headline Summary */}
          {alert.headline && (
            <div className="alert-headline-box" data-testid="alert-detail-headline">
              <strong>Summary:</strong> {alert.headline}
            </div>
          )}

          {/* Safety Instructions */}
          {alert.instruction && (
            <div className="alert-instructions-box" data-testid="alert-detail-instruction">
              <div className="instruction-header">🛡️ RECOMMENDED ACTION / SAFETY GUIDANCE:</div>
              <p>{alert.instruction}</p>
            </div>
          )}

          {/* Full Meteorological Statement */}
          <div className="alert-description-box" data-testid="alert-detail-description">
            <div className="description-header">METEOROLOGICAL STATEMENT & DETAIL:</div>
            <p className="description-text">{alert.description}</p>
          </div>
        </div>

        <div className="modal-footer">
          <button
            type="button"
            className="btn-primary"
            onClick={onClose}
            data-testid="alert-dismiss-btn"
          >
            Acknowledge & Close
          </button>
        </div>
      </div>
    </div>
  );
};
