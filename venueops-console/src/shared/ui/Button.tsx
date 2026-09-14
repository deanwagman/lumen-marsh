import type { ButtonHTMLAttributes, ReactNode } from 'react';

import styles from './Button.module.css';

type ButtonVariant = 'default' | 'danger' | 'hold' | 'ghost';

export function Button({
  children,
  variant = 'default',
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  children: ReactNode;
  variant?: ButtonVariant;
}) {
  const classes = [styles.button, styles[variant], className].filter(Boolean).join(' ');

  return (
    <button type="button" className={classes} {...props}>
      {children}
    </button>
  );
}
