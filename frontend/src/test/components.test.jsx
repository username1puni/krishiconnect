import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import i18n from '../i18n'
import DistanceInfo from '../components/DistanceInfo.jsx'
import Login from '../pages/Login.jsx'
import { AuthProvider } from '../context/AuthContext.jsx'

afterEach(async () => {
  cleanup()
  await i18n.changeLanguage('en')
})

describe('DistanceInfo', () => {
  it('shows road distance with driving time', () => {
    render(<DistanceInfo distanceKm={12.4} durationMinutes={21} source="ROAD" />)
    expect(screen.getByText('12 km')).toBeInTheDocument()
    expect(screen.getByText(/by road · 21 min/)).toBeInTheDocument()
  })

  it('says when it fell back to straight-line distance', () => {
    render(<DistanceInfo distanceKm={8.81} source="STRAIGHT_LINE" />)
    expect(screen.getByText('8.8 km')).toBeInTheDocument()
    expect(screen.getByText('straight line')).toBeInTheDocument()
  })

  it('renders in Kannada', async () => {
    await i18n.changeLanguage('kn')
    render(<DistanceInfo distanceKm={5} source="ROAD" />)
    expect(screen.getByText('ರಸ್ತೆ ಮೂಲಕ')).toBeInTheDocument()
  })
})

describe('Login page', () => {
  it('fills a demo account when its button is pressed', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <Login />
        </AuthProvider>
      </MemoryRouter>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Equipment owner' }))
    expect(screen.getByLabelText('Mobile number or email')).toHaveValue('9900000002')
    expect(screen.getByLabelText('Password')).toHaveValue('demo1234')
  })

  it('switches to Hindi', async () => {
    await i18n.changeLanguage('hi')
    render(
      <MemoryRouter>
        <AuthProvider>
          <Login />
        </AuthProvider>
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: 'लॉग इन' })).toBeInTheDocument()
  })
})
