import React from 'react';
import { WeatherAlert } from '../types/weather';

interface WeatherAlertsBannerProps {
  alerts: WeatherAlert[];
  onSelectAlert: (alert: WeatherAlert) => void;
}

export const WeatherAlertsBanner: React.FC<WeatherAlertsBannerProps> = ({
  alerts,
  onSelectAlert,
}) => {
  if (!alerts || alerts.length === 0) return null;

  // Prioritize the most severe alert
  const sortedAlerts = [...alerts].sort((a, b) => {
    const weights: Record<WeatherAlert['severity'], number> = {
      Extreme: 4,
      Severe: 3,
      Moderate: 2,
      Minor: 1,
    };
    return (weights[b.severity] || 0) - (weights[a.severity] || 0);
  });

  const topAlert = sortedAlerts[0];
  const severityClass = `alert-${topAlert.severity.toLowerCase()}`;
  const icon = topAlert.severity === 'Extreme' ? '🚨' : topAlert.severity === 'Severe' ? '⚡' : '⚠️';

  return (
    <div
      className={`weather-alert-banner ${severityClass}`}
      onClick={() => onSelectAlert(topAlert)}
      role="alert"
      data-testid="weather-alert-banner"
      title="Tap to view full emergency weather alert details"
    >
      <div className="alert-pulse-dot" />
      <div className="alert-icon">{icon}</div>
      <div className="alert-content">
        <div className="alert-event-title" data-testid="alert-event-title">
          {topAlert.event.toUpperCase()}
          {alerts.length > 1 && <span className="alert-counter-badge">+{alerts.length - 1} more</span>}
        </div>
        <div className="alert-headline" data-testid="alert-headline">
          {topAlert.headline}
        </div>
      </div>
      <button
        type="button"
        className="alert-details-btn"
        data-testid="alert-details-btn"
      >
        DETAILS →
      </button>
    </div>
  );
};
