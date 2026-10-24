/*
  StylePicker: the five styles as tappable cards, each with a tiny preview
  of its colors and lettering. Used when creating an account and in
  Profile › Appearance.
*/
import { STYLES } from '../lib/styles'
import type { AppStyle } from '../types'

export function StylePicker({ value, onChange }: { value: AppStyle; onChange: (style: AppStyle) => void }) {
  return (
    <div className="style-picker" role="radiogroup" aria-label="Style">
      {STYLES.map((style) => {
        const [background, ink, accent, gold] = style.swatches
        return (
          <label key={style.id} className={value === style.id ? 'style-option chosen' : 'style-option'}>
            <input
              type="radio"
              name="style"
              value={style.id}
              checked={value === style.id}
              onChange={() => onChange(style.id)}
            />
            {/* The preview: the style's background, its lettering and two accents. */}
            <span className={`style-mini mini-${style.id.toLowerCase()}`} style={{ background, color: ink }} aria-hidden="true">
              <b>Aa</b>
              <i style={{ background: accent }} />
              <i style={{ background: gold }} />
            </span>
            <span className="style-text">
              <span className="style-name">{style.name}</span>
              <span className="style-about">{style.about}</span>
            </span>
          </label>
        )
      })}
    </div>
  )
}
