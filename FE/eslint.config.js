import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'

export default [
  { ignores: ['dist/**', 'legacy/**'] },
  { files: ['src/**/*.{js,jsx}'], languageOptions: { ecmaVersion: 'latest', sourceType: 'module', parserOptions: { ecmaFeatures: { jsx: true } }, globals: { ...globals.browser } }, plugins: { 'react-hooks': reactHooks }, rules: { ...js.configs.recommended.rules, ...reactHooks.configs.flat.recommended.rules, 'react-hooks/refs': 'off', 'react-hooks/set-state-in-effect': 'off', 'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }] } }
]
