/** Título e subtítulo no começo de cada tela (como na maquete). */
export function CabecalhoPagina({ titulo, subtitulo }: { titulo: string; subtitulo?: string }) {
  return (
    <div className="pagina-cabecalho">
      <h1 className="pagina-titulo">{titulo}</h1>
      {subtitulo && <p className="pagina-subtitulo">{subtitulo}</p>}
    </div>
  )
}
