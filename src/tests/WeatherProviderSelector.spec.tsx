import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { WeatherProviderSelector } from '../components/WeatherProviderSelector';

describe('WeatherProviderSelector Component', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('renders active provider name and badge in collapsed state', () => {
    render(
      <WeatherProviderSelector
        currentProvider="open-meteo"
        onProviderChange={vi.fn()}
      />
    );

    expect(screen.getByTestId('current-provider-name')).toHaveTextContent('Open-Meteo');
    expect(screen.getByTestId('current-provider-badge')).toHaveTextContent('FREE & OPEN');
    expect(screen.queryByTestId('provider-dropdown')).not.toBeInTheDocument();
  });

  it('opens dropdown when toggle is clicked, showing all 5 providers', () => {
    render(
      <WeatherProviderSelector
        currentProvider="open-meteo"
        onProviderChange={vi.fn()}
      />
    );

    const toggle = screen.getByTestId('provider-toggle');
    fireEvent.click(toggle);

    expect(screen.getByTestId('provider-dropdown')).toBeInTheDocument();
    expect(screen.getByTestId('provider-option-open-meteo')).toBeInTheDocument();
    expect(screen.getByTestId('provider-option-nws')).toBeInTheDocument();
    expect(screen.getByTestId('provider-option-met-norway')).toBeInTheDocument();
    expect(screen.getByTestId('provider-option-openweathermap')).toBeInTheDocument();
    expect(screen.getByTestId('provider-option-weatherapi')).toBeInTheDocument();
  });

  it('triggers onProviderChange when another provider is selected', () => {
    const handleProviderChange = vi.fn();
    render(
      <WeatherProviderSelector
        currentProvider="open-meteo"
        onProviderChange={handleProviderChange}
      />
    );

    fireEvent.click(screen.getByTestId('provider-toggle'));
    fireEvent.click(screen.getByTestId('provider-option-nws'));

    expect(handleProviderChange).toHaveBeenCalledWith('nws');
  });

  it('shows API key input for key-requiring provider and saves key', () => {
    render(
      <WeatherProviderSelector
        currentProvider="openweathermap"
        onProviderChange={vi.fn()}
      />
    );

    // Open dropdown
    fireEvent.click(screen.getByTestId('provider-toggle'));

    expect(screen.getByTestId('provider-key-box')).toBeInTheDocument();

    const input = screen.getByTestId('api-key-input');
    fireEvent.change(input, { target: { value: 'test_owm_secret_key_123' } });

    const saveBtn = screen.getByTestId('save-key-btn');
    fireEvent.click(saveBtn);

    expect(screen.getByTestId('key-saved-notice')).toHaveTextContent('Key saved securely');
    expect(localStorage.getItem('owm_api_key')).toBe('test_owm_secret_key_123');
  });
});
