import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { WeatherAlertsBanner } from '../components/WeatherAlertsBanner';
import { AlertDetailsModal } from '../components/AlertDetailsModal';
import { WeatherAlert } from '../types/weather';

const mockAlerts: WeatherAlert[] = [
  {
    id: 'alert-mod-1',
    event: 'Wind Advisory',
    severity: 'Minor',
    headline: 'Brisk winds 20-30 mph',
    description: 'Gusty winds expected throughout the afternoon.',
    source: 'WMO_METEOROLOGICAL',
  },
  {
    id: 'alert-sev-1',
    event: 'Tornado Warning',
    severity: 'Extreme',
    urgency: 'Immediate',
    headline: 'Confirmed tornado on the ground near Yorktown',
    description: 'Flying debris will be dangerous to those caught without shelter.',
    instruction: 'Take shelter immediately in a basement or interior room.',
    onset: '2026-10-21T03:00:00Z',
    expires: '2026-10-21T04:00:00Z',
    senderName: 'National Weather Service',
    source: 'NOAA_NWS',
  },
];

describe('WeatherAlertsBanner & AlertDetailsModal', () => {
  it('renders nothing if alerts array is empty or null', () => {
    const { container: c1 } = render(
      <WeatherAlertsBanner alerts={[]} onSelectAlert={vi.fn()} />
    );
    expect(c1.firstChild).toBeNull();
  });

  it('renders top severity alert first with counter badge and triggers selection', () => {
    const handleSelect = vi.fn();
    render(
      <WeatherAlertsBanner alerts={mockAlerts} onSelectAlert={handleSelect} />
    );

    const banner = screen.getByTestId('weather-alert-banner');
    expect(banner).toBeInTheDocument();
    expect(banner.className).toContain('alert-extreme');

    // Title displays the Extreme alert (Tornado Warning)
    expect(screen.getByTestId('alert-event-title')).toHaveTextContent(/TORNADO WARNING/i);
    expect(screen.getByTestId('alert-event-title')).toHaveTextContent('+1 more');
    expect(screen.getByTestId('alert-headline')).toHaveTextContent('Confirmed tornado on the ground');

    // Clicking banner or details button triggers select
    fireEvent.click(screen.getByTestId('alert-details-btn'));
    expect(handleSelect).toHaveBeenCalledWith(mockAlerts[1]); // Extreme alert
  });

  it('renders AlertDetailsModal when open and shows full emergency statements', () => {
    const handleClose = vi.fn();
    render(
      <AlertDetailsModal
        isOpen={true}
        onClose={handleClose}
        alert={mockAlerts[1]}
      />
    );

    const modal = screen.getByTestId('alert-details-modal');
    expect(modal).toBeInTheDocument();
    expect(screen.getByTestId('alert-modal-title')).toHaveTextContent('Tornado Warning');
    expect(screen.getByTestId('alert-severity-badge')).toHaveTextContent('EXTREME');
    expect(screen.getByText(/Take shelter immediately/i)).toBeInTheDocument();
    expect(screen.getByText(/Flying debris will be dangerous/i)).toBeInTheDocument();
    expect(screen.getByText(/National Weather Service/i)).toBeInTheDocument();

    // Close button triggers onClose
    fireEvent.click(screen.getByTestId('close-alert-modal-btn'));
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('does not render AlertDetailsModal when isOpen is false', () => {
    const { container } = render(
      <AlertDetailsModal
        isOpen={false}
        onClose={vi.fn()}
        alert={mockAlerts[1]}
      />
    );
    expect(container.firstChild).toBeNull();
  });
});
