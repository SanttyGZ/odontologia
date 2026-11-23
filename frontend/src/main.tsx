import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

const root = document.getElementById('root');
if (root === null) {
  throw new Error('Falta el elemento raíz de la aplicación.');
}

// C-01 solo prepara el punto de entrada; las pantallas pertenecen a C-12 a C-15.
createRoot(root).render(<StrictMode>{null}</StrictMode>);
