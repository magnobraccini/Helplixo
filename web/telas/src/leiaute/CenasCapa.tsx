// GERADO a partir de web/docs/maquete-v32.dc.html (linhas 101–420) — não edite à mão.
// As cenas ilustradas da capa, uma por aba. Cores vindas da paleta da capa (CapaPaleta).
import type { CapaPaleta, Cena } from './capaPaletas'

export function CenasCapa({ cena, noite, t }: { cena: Cena; noite: boolean; t: CapaPaleta }) {
  return (
    <>
      {/* INÍCIO: panorama com colinas, sol/lua e o macaco atendendo */}
      {cena === 'inicio' && (<>
      <g>
      <path d="M0 214 C180 160 340 196 520 176 C700 156 820 206 1000 186 C1180 166 1320 196 1440 176 L1440 300 L0 300 Z" fill={t.sage} opacity="0.5" />
      <path d="M0 262 C220 222 420 252 640 238 C860 224 1060 264 1260 246 C1340 240 1400 242 1440 238 L1440 300 L0 300 Z" fill={t.sage} stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" />
      <g fill={t.card} stroke={t.ink} strokeWidth="2.4" strokeLinejoin="round">
      <path d="M560 86 C560 68 582 62 596 72 C604 56 632 56 638 74 C654 72 664 86 654 98 L566 98 C556 98 554 88 560 86 Z" />
      <path d="M1290 214 C1290 200 1308 196 1318 204 C1324 192 1346 192 1350 206 C1362 204 1370 216 1362 224 L1296 224 C1288 224 1286 216 1290 214 Z" />
      </g>
      <g fill="none" stroke={t.ink} strokeWidth="2.4" strokeLinecap="round">
      <path d="M700 70 q10 -10 20 0 q10 -10 20 0" /><path d="M752 92 q8 -8 16 0 q8 -8 16 0" />
      <path d="M300 40 C400 18 470 60 560 34" strokeDasharray="1 10" opacity="0.5" />
      </g>
      {/* ZENAS: o macaco do Helplixo, de headset, segurando o café com as duas mãos */}
      <path d="M896 250 C856 176 896 66 990 54 C1094 42 1162 108 1142 182 C1126 244 1050 276 966 272 C934 270 910 264 896 250 Z" fill={t.blobA} />
      <path d="M960 288 C912 296 886 266 900 240 C912 220 940 226 936 246" fill="none" stroke={t.skin} strokeWidth="9" strokeLinecap="round" transform="translate(5 5)" />
      <path d="M960 288 C912 296 886 266 900 240 C912 220 940 226 936 246" fill="none" stroke={t.ink} strokeWidth="2.6" strokeLinecap="round" />
      <g transform="translate(6 6)">
      <path d="M950 300 C946 250 968 212 1012 210 C1056 212 1078 250 1074 300 Z" fill={t.blobB} />
      <circle cx="940" cy="146" r="22" fill={t.skin} /><circle cx="1084" cy="146" r="22" fill={t.skin} />
      <path d="M950 142 C948 100 980 76 1014 77 C1050 78 1076 104 1074 144 C1072 184 1046 204 1012 203 C976 202 952 182 950 142 Z" fill={t.skin} />
      <path d="M974 154 C970 126 992 116 1012 130 C1032 116 1054 126 1050 154 C1048 182 1030 194 1012 194 C994 194 976 182 974 154 Z" fill={t.mask} />
      <rect x="926" y="122" width="20" height="38" rx="9" fill={t.sage} /><rect x="1078" y="122" width="20" height="38" rx="9" fill={t.sage} />
      <path d="M838 220 C816 212 808 192 814 182 C832 188 842 202 838 220 Z" fill={t.sage} />
      <path d="M848 196 C866 182 884 186 888 196 C872 206 858 206 848 196 Z" fill={t.sage} />
      </g>
      <g fill="none" stroke={t.ink} strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M950 300 C946 250 968 212 1012 210 C1056 212 1078 250 1074 300" />
      <path d="M986 214 Q1012 230 1038 214" />
      <circle cx="940" cy="146" r="22" /><circle cx="940" cy="146" r="10" />
      <circle cx="1084" cy="146" r="22" /><circle cx="1084" cy="146" r="10" />
      <path d="M950 142 C948 100 980 76 1014 77 C1050 78 1076 104 1074 144 C1072 184 1046 204 1012 203 C976 202 952 182 950 142 Z" />
      <path d="M992 84 C998 74 1006 80 1010 72 C1014 82 1024 76 1028 86" />
      <path d="M974 154 C970 126 992 116 1012 130 C1032 116 1054 126 1050 154 C1048 182 1030 194 1012 194 C994 194 976 182 974 154 Z" />
      <path d="M998 176 Q1012 188 1026 176" />
      <path d="M942 132 C946 58 1078 58 1082 132" strokeWidth="3.4" />
      <rect x="926" y="122" width="20" height="38" rx="9" /><rect x="1078" y="122" width="20" height="38" rx="9" />
      <path d="M934 158 C928 192 954 206 984 198" />
      <path d="M850 280 C846 236 854 200 866 166" />
      <path d="M838 220 C816 212 808 192 814 182 C832 188 842 202 838 220 Z" />
      <path d="M848 196 C866 182 884 186 888 196 C872 206 858 206 848 196 Z" />
      </g>
      {/* braços, mãos e xícara: na frente do corpo, com preenchimento no lugar */}
      <g stroke={t.ink} strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M958 226 C944 252 958 278 994 274 L998 258 C978 260 970 250 976 236 Z" fill={t.skin} />
      <path d="M1066 226 C1080 252 1066 278 1030 274 L1026 258 C1046 260 1054 250 1048 236 Z" fill={t.skin} />
      <path d="M992 236 L995 272 Q997 282 1008 282 L1016 282 Q1027 282 1029 272 L1032 236 Z" fill={t.card} />
      <path d="M992 246 H1032" fill="none" opacity="0.5" />
      <path d="M1031 244 C1046 244 1046 266 1029 264" fill="none" />
      <ellipse cx="994" cy="264" rx="10" ry="9" fill={t.mask} />
      <ellipse cx="1030" cy="264" rx="10" ry="9" fill={t.mask} />
      <path d="M1004 228 C998 218 1010 212 1004 202" fill="none" />
      <path d="M1020 228 C1014 218 1026 212 1020 202" fill="none" opacity="0.8" />
      </g>
      <g fill={t.ink}>
      <circle cx="996" cy="146" r="4.6" /><circle cx="1028" cy="146" r="4.6" />
      <circle cx="1006" cy="166" r="2.2" /><circle cx="1018" cy="166" r="2.2" />
      <circle cx="986" cy="198" r="5.5" />
      <path d="M500 150 L502 158 L510 160 L502 162 L500 170 L498 162 L490 160 L498 158 Z" opacity="0.6" />
      </g>
      <text x="1108" y="232" fontFamily="Caveat, cursive" fontSize="30" fontWeight="600" fill={t.ink}>Zenas</text>
      <path d="M1104 222 C1090 214 1084 206 1086 194" fill="none" stroke={t.ink} strokeWidth="2.2" strokeLinecap="round" />
      <path d="M1080 200 L1086 192 L1093 199" fill="none" stroke={t.ink} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
      {noite && (<>
      <path d="M800 70 A30 30 0 1 0 828 112 A23 23 0 1 1 800 70 Z" fill={t.blobA} stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" />
      </>)}
      {!noite && (<>
      <g stroke={t.ink} strokeWidth="2.6" strokeLinecap="round">
      <circle cx="812" cy="96" r="24" fill={t.blobA} />
      <path d="M812 58v-12M812 146v-12M774 96h-12M862 96h-12M785 69l-8-8M847 131l-8-8M785 123l-8 8M847 61l-8 8" />
      </g>
      </>)}
      </g>
      </>)}
      
      {/* PROCESSOS: uma trilha com pedras numeradas até a bandeira */}
      {cena === 'processos' && (<>
      <g>
      <path d="M0 250 C240 224 460 260 720 246 C980 232 1200 262 1440 240 L1440 300 L0 300 Z" fill={t.sage} stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" />
      <path d="M40 262 C200 210 300 120 470 140 C640 160 660 236 820 222 C980 208 1000 112 1160 118 C1280 124 1320 176 1380 150" fill="none" stroke={t.ink} strokeWidth="3" strokeLinecap="round" strokeDasharray="2 13" />
      <g transform="translate(6 6)" fill={t.blobB}>
      <circle cx="240" cy="190" r="26" /><circle cx="470" cy="140" r="26" /><circle cx="820" cy="222" r="26" /><circle cx="1160" cy="118" r="26" />
      </g>
      <g fill={t.blobA} stroke={t.ink} strokeWidth="2.6">
      <circle cx="240" cy="190" r="26" /><circle cx="470" cy="140" r="26" /><circle cx="820" cy="222" r="26" /><circle cx="1160" cy="118" r="26" />
      </g>
      <g fill={t.ink} fontFamily="Fraunces, Georgia, serif" fontSize="24" fontWeight="600" textAnchor="middle">
      <text x="240" y="199">1</text><text x="470" y="149">2</text><text x="820" y="231">3</text><text x="1160" y="127">4</text>
      </g>
      <g stroke={t.ink} strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1380 152 L1380 64" fill="none" />
      <path d="M1380 66 L1430 82 L1380 98 Z" fill={t.blobB} />
      <path d="M640 240 L640 210" fill="none" /><circle cx="640" cy="196" r="20" fill={t.sage} />
      <path d="M1000 236 L1000 200" fill="none" /><circle cx="1000" cy="184" r="24" fill={t.sage} />
      <path d="M120 254 L120 226" fill="none" /><circle cx="120" cy="212" r="18" fill={t.sage} />
      <rect x="320" y="34" width="58" height="72" rx="6" fill={t.card} transform="rotate(-8 349 70)" />
      <path d="M334 56h28M334 70h30M334 84h20" fill="none" transform="rotate(-8 349 70)" />
      <rect x="1240" y="190" width="54" height="66" rx="6" fill={t.card} transform="rotate(6 1267 223)" />
      <path d="M1252 210h26M1252 224h28M1252 238h18" fill="none" transform="rotate(6 1267 223)" />
      </g>
      <g fill={t.ink}><path d="M620 60 L623 72 L635 75 L623 78 L620 90 L617 78 L605 75 L617 72 Z" /><path d="M1320 40 L1322 48 L1330 50 L1322 52 L1320 60 L1318 52 L1310 50 L1318 48 Z" /></g>
      </g>
      </>)}
      
      {/* CONTATOS: fio de telefone, telefone, balões com ramais e um cartão de visita (externos) */}
      {cena === 'contatos' && (<>
      <g>
      <g transform="rotate(-8 210 84)" stroke={t.ink} strokeWidth="2.4" strokeLinejoin="round" strokeLinecap="round">
      <rect x="120" y="36" width="180" height="96" rx="8" fill={t.blobA} transform="translate(6 6)" stroke="none" />
      <rect x="120" y="36" width="180" height="96" rx="8" fill={t.card} />
      <circle cx="158" cy="74" r="17" fill={t.sage} />
      <path d="M188 66 H280 M188 82 H262 M138 110 H280" fill="none" />
      </g>
      <path d="M0 206 C30 156 70 156 100 206 S170 256 200 206 S270 156 300 206 S370 256 400 206 S470 156 500 206 S570 256 600 206 S670 156 700 206 S770 256 800 206 S870 156 900 206 S970 256 1000 206" fill="none" stroke={t.blobB} strokeWidth="6" strokeLinecap="round" transform="translate(5 5)" />
      <path d="M0 206 C30 156 70 156 100 206 S170 256 200 206 S270 156 300 206 S370 256 400 206 S470 156 500 206 S570 256 600 206 S670 156 700 206 S770 256 800 206 S870 156 900 206 S970 256 1000 206" fill="none" stroke={t.ink} strokeWidth="2.6" strokeLinecap="round" />
      <g transform="translate(6 6)">
      <path d="M1000 262 C1004 214 1022 196 1100 196 C1178 196 1196 214 1200 262 Z" fill={t.blobA} />
      <path d="M988 186 C988 152 1024 148 1036 164 L1164 164 C1176 148 1212 152 1212 186 C1212 198 1188 200 1180 188 L1020 188 C1012 200 988 198 988 186 Z" fill={t.blobB} />
      </g>
      <g stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" strokeLinecap="round">
      <path d="M1000 262 C1004 214 1022 196 1100 196 C1178 196 1196 214 1200 262 Z" fill={t.blobA} />
      <path d="M988 186 C988 152 1024 148 1036 164 L1164 164 C1176 148 1212 152 1212 186 C1212 198 1188 200 1180 188 L1020 188 C1012 200 988 198 988 186 Z" fill={t.blobB} />
      <circle cx="1100" cy="232" r="22" fill={t.card} />
      <circle cx="1100" cy="232" r="6" fill="none" />
      </g>
      <g fill={t.ink}><circle cx="1100" cy="216" r="2.4" /><circle cx="1114" cy="222" r="2.4" /><circle cx="1117" cy="236" r="2.4" /><circle cx="1086" cy="222" r="2.4" /><circle cx="1083" cy="236" r="2.4" /></g>
      <g stroke={t.ink} strokeWidth="2.4" strokeLinejoin="round" fill={t.card}>
      <path d="M520 50 H640 A14 14 0 0 1 654 64 V96 A14 14 0 0 1 640 110 H566 L548 128 L550 110 H520 A14 14 0 0 1 506 96 V64 A14 14 0 0 1 520 50 Z" />
      <path d="M800 92 H910 A14 14 0 0 1 924 106 V136 A14 14 0 0 1 910 150 H860 L846 166 L846 150 H800 A14 14 0 0 1 786 136 V106 A14 14 0 0 1 800 92 Z" />
      <path d="M1260 40 H1370 A14 14 0 0 1 1384 54 V84 A14 14 0 0 1 1370 98 H1330 L1318 114 L1316 98 H1260 A14 14 0 0 1 1246 84 V54 A14 14 0 0 1 1260 40 Z" />
      </g>
      <g fill={t.ink} fontFamily="Fraunces, Georgia, serif" fontWeight="600" textAnchor="middle">
      <text x="580" y="90" fontSize="28">2214</text><text x="855" y="130" fontSize="26">2001</text><text x="1315" y="78" fontSize="26">2310</text>
      </g>
      <g fill="none" stroke={t.ink} strokeWidth="2.4" strokeLinecap="round"><path d="M1226 150 q14 -10 10 -26" /><path d="M1240 160 q24 -16 18 -42" /></g>
      <g fill={t.ink}><path d="M720 40 L723 52 L735 55 L723 58 L720 70 L717 58 L705 55 L717 52 Z" /></g>
      </g>
      </>)}
      
      {/* AVISOS: bandeirinhas, sino tocando, megafone e bilhetes presos */}
      {cena === 'avisos' && (<>
      <g>
      <path d="M0 30 C360 112 1080 112 1440 30" fill="none" stroke={t.ink} strokeWidth="2.4" />
      <g stroke={t.ink} strokeWidth="2.2" strokeLinejoin="round">
      <path d="M98 50 L142 56 L118 98 Z" fill={t.blobA} />
      <path d="M218 68 L262 76 L238 118 Z" fill={t.blobB} />
      <path d="M338 82 L382 88 L358 130 Z" fill={t.sage} />
      <path d="M458 92 L502 96 L478 138 Z" fill={t.blobA} />
      <path d="M578 98 L622 100 L598 142 Z" fill={t.blobB} />
      <path d="M698 100 L742 100 L720 142 Z" fill={t.sage} />
      <path d="M818 100 L862 98 L842 142 Z" fill={t.blobA} />
      <path d="M938 96 L982 92 L962 138 Z" fill={t.blobB} />
      <path d="M1058 88 L1102 82 L1082 130 Z" fill={t.sage} />
      <path d="M1178 76 L1222 68 L1202 118 Z" fill={t.blobA} />
      <path d="M1298 56 L1342 50 L1322 98 Z" fill={t.blobB} />
      </g>
      <path d="M0 270 C300 250 700 278 1000 262 C1200 252 1340 266 1440 258 L1440 300 L0 300 Z" fill={t.sage} opacity="0.6" />
      <g transform="translate(6 6)">
      <path d="M560 260 C564 206 578 184 606 184 C634 184 648 206 652 260 Z" fill={t.blobB} />
      <path d="M1000 210 L1110 166 L1110 280 L1000 240 Z" fill={t.blobA} />
      </g>
      <g stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" strokeLinecap="round">
      <path d="M560 260 C564 206 578 184 606 184 C634 184 648 206 652 260 Z" fill={t.blobB} />
      <path d="M548 260 H664" fill="none" /><circle cx="606" cy="270" r="9" fill={t.card} /><path d="M606 184 V172" fill="none" />
      <path d="M530 196 q-12 16 -4 34" fill="none" /><path d="M682 196 q12 16 4 34" fill="none" />
      <path d="M1000 210 L1110 166 L1110 280 L1000 240 Z" fill={t.blobA} />
      <rect x="974" y="206" width="28" height="38" rx="6" fill={t.card} />
      <path d="M1012 244 L1024 282 L1044 276 L1034 250" fill={t.card} />
      <path d="M1132 196 q16 26 0 52" fill="none" /><path d="M1152 182 q26 40 0 80" fill="none" />
      <rect x="760" y="176" width="92" height="84" rx="4" fill={t.card} transform="rotate(-6 806 218)" />
      <path d="M776 206h56M776 222h60M776 238h40" fill="none" transform="rotate(-6 806 218)" />
      <rect x="1260" y="170" width="86" height="80" rx="4" fill={t.card} transform="rotate(7 1303 210)" />
      <path d="M1276 198h52M1276 214h56M1276 230h34" fill="none" transform="rotate(7 1303 210)" />
      <circle cx="806" cy="178" r="7" fill={t.blobB} /><circle cx="1303" cy="172" r="7" fill={t.blobB} />
      </g>
      </g>
      </>)}
      
      {/* FERRAMENTAS: painel furado com chave, chave de fenda, martelo e engrenagens */}
      {cena === 'ferramentas' && (<>
      <g>
      <defs><pattern id="furos" width="28" height="28" patternUnits="userSpaceOnUse"><circle cx="14" cy="14" r="2.6" fill={t.ink} opacity="0.18" /></pattern></defs>
      <rect x="0" y="0" width="1440" height="300" fill="url(#furos)" />
      <g transform="rotate(-28 520 160)">
      <rect x="446" y="156" width="150" height="24" rx="12" fill={t.blobB} transform="translate(6 6)" />
      <rect x="446" y="156" width="150" height="24" rx="12" fill={t.blobA} stroke={t.ink} strokeWidth="2.6" />
      <path d="M596 140 a32 32 0 1 1 0 56 l0 -14 a18 18 0 1 0 0 -28 Z" fill={t.blobA} stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" />
      </g>
      <g transform="rotate(18 760 150)">
      <rect x="690" y="134" width="70" height="32" rx="12" fill={t.blobB} stroke={t.ink} strokeWidth="2.6" />
      <path d="M702 140v20M716 140v20M730 140v20" stroke={t.ink} strokeWidth="2" opacity="0.5" />
      <rect x="760" y="145" width="110" height="10" rx="3" fill={t.card} stroke={t.ink} strokeWidth="2.4" />
      <path d="M870 145 L888 150 L870 155 Z" fill={t.ink} />
      </g>
      <g transform="rotate(-10 960 190)">
      <rect x="950" y="150" width="20" height="130" rx="9" fill={t.blobA} stroke={t.ink} strokeWidth="2.6" />
      <rect x="912" y="118" width="96" height="38" rx="7" fill={t.sage} stroke={t.ink} strokeWidth="2.6" />
      </g>
      <circle cx="1186" cy="146" r="50" fill="none" stroke={t.blobB} strokeWidth="18" strokeDasharray="14 11" transform="translate(6 6)" />
      <circle cx="1186" cy="146" r="50" fill="none" stroke={t.ink} strokeWidth="18" strokeDasharray="14 11" />
      <circle cx="1186" cy="146" r="40" fill={t.blobA} stroke={t.ink} strokeWidth="2.6" />
      <circle cx="1186" cy="146" r="13" fill={t.card} stroke={t.ink} strokeWidth="2.6" />
      <circle cx="1296" cy="226" r="28" fill="none" stroke={t.ink} strokeWidth="12" strokeDasharray="9 8" />
      <circle cx="1296" cy="226" r="22" fill={t.sage} stroke={t.ink} strokeWidth="2.4" />
      <circle cx="1296" cy="226" r="7" fill={t.card} stroke={t.ink} strokeWidth="2.4" />
      <g fill={t.ink}><path d="M660 50 L663 62 L675 65 L663 68 L660 80 L657 68 L645 65 L657 62 Z" /><path d="M1080 250 L1082 258 L1090 260 L1082 262 L1080 270 L1078 262 L1070 260 L1078 258 Z" /></g>
      </g>
      </>)}
      
      {/* CHAMADOS: fila de tíquetes picotados indo para a caixa de entrada */}
      {cena === 'chamados' && (<>
      <g>
      <path d="M0 266 C300 246 700 276 1000 258 C1200 248 1340 262 1440 254 L1440 300 L0 300 Z" fill={t.sage} opacity="0.55" />
      <path d="M480 236 C700 266 900 180 1140 214" fill="none" stroke={t.ink} strokeWidth="2.6" strokeLinecap="round" strokeDasharray="2 12" />
      <path d="M1128 204 L1146 216 L1126 226" fill="none" stroke={t.ink} strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
      <g transform="translate(540 150) rotate(-8)">
      <path d="M0 0 H130 V24 A11 11 0 0 0 130 46 V70 H0 V46 A11 11 0 0 0 0 24 Z" fill={t.blobB} transform="translate(6 6)" />
      <path d="M0 0 H130 V24 A11 11 0 0 0 130 46 V70 H0 V46 A11 11 0 0 0 0 24 Z" fill={t.card} stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" />
      <path d="M96 8 V62" stroke={t.ink} strokeWidth="2" strokeDasharray="3 5" />
      <text x="14" y="32" fontFamily="Fraunces, Georgia, serif" fontSize="20" fontWeight="600" fill={t.ink}>#1042</text>
      <path d="M14 48 H76" stroke={t.ink} strokeWidth="2.4" strokeLinecap="round" />
      </g>
      <g transform="translate(720 96) rotate(6)">
      <path d="M0 0 H130 V24 A11 11 0 0 0 130 46 V70 H0 V46 A11 11 0 0 0 0 24 Z" fill={t.blobA} transform="translate(6 6)" />
      <path d="M0 0 H130 V24 A11 11 0 0 0 130 46 V70 H0 V46 A11 11 0 0 0 0 24 Z" fill={t.card} stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" />
      <path d="M96 8 V62" stroke={t.ink} strokeWidth="2" strokeDasharray="3 5" />
      <text x="14" y="32" fontFamily="Fraunces, Georgia, serif" fontSize="20" fontWeight="600" fill={t.ink}>#1043</text>
      <path d="M14 48 H70" stroke={t.ink} strokeWidth="2.4" strokeLinecap="round" />
      </g>
      <g transform="translate(900 150) rotate(-4)">
      <path d="M0 0 H130 V24 A11 11 0 0 0 130 46 V70 H0 V46 A11 11 0 0 0 0 24 Z" fill={t.sage} transform="translate(6 6)" />
      <path d="M0 0 H130 V24 A11 11 0 0 0 130 46 V70 H0 V46 A11 11 0 0 0 0 24 Z" fill={t.card} stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" />
      <path d="M96 8 V62" stroke={t.ink} strokeWidth="2" strokeDasharray="3 5" />
      <text x="14" y="32" fontFamily="Fraunces, Georgia, serif" fontSize="20" fontWeight="600" fill={t.ink}>#1044</text>
      <path d="M14 48 H82" stroke={t.ink} strokeWidth="2.4" strokeLinecap="round" />
      </g>
      <g transform="translate(6 6)"><path d="M1160 200 L1340 200 L1364 262 L1136 262 Z" fill={t.blobB} /></g>
      <g stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" strokeLinecap="round">
      <rect x="1196" y="150" width="100" height="62" rx="4" fill={t.card} transform="rotate(-6 1246 181)" />
      <path d="M1160 200 L1340 200 L1364 262 L1136 262 Z" fill={t.blobA} />
      <path d="M1136 232 H1196 L1206 246 H1294 L1304 232 H1364" fill="none" />
      <circle cx="1360" cy="96" r="30" fill={t.sage} />
      <path d="M1346 96 L1356 106 L1376 84" fill="none" strokeWidth="3.4" />
      </g>
      <g fill={t.ink}><path d="M1060 50 L1063 62 L1075 65 L1063 68 L1060 80 L1057 68 L1045 65 L1057 62 Z" /><path d="M640 40 L642 48 L650 50 L642 52 L640 60 L638 52 L630 50 L638 48 Z" /></g>
      </g>
      </>)}
      
      {/* INVENTÁRIO: monitor e gabinete com etiqueta de patrimônio, trilha até o carrinho do depósito */}
      {cena === 'inventario' && (<>
      <g>
      <path d="M0 262 C300 244 700 272 1000 256 C1200 248 1340 260 1440 252 L1440 300 L0 300 Z" fill={t.sage} opacity="0.55" />
      <g transform="translate(6 6)">
      <rect x="250" y="70" width="230" height="150" rx="10" fill={t.blobB} />
      <rect x="560" y="96" width="74" height="160" rx="8" fill={t.blobA} />
      <rect x="1150" y="150" width="120" height="96" rx="4" fill={t.blobB} />
      <rect x="1170" y="92" width="86" height="60" rx="4" fill={t.sage} />
      </g>
      <g stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" strokeLinecap="round">
      <rect x="250" y="70" width="230" height="150" rx="10" fill={t.card} />
      <rect x="266" y="86" width="198" height="112" rx="4" fill={t.blobA} />
      <path d="M290 120 H380 M290 140 H420 M290 160 H350" fill="none" opacity="0.6" />
      <path d="M352 220 L344 254 M378 220 L386 254 M320 256 H410" fill="none" />
      <rect x="560" y="96" width="74" height="160" rx="8" fill={t.card} />
      <path d="M576 120 H618 M576 136 H618" fill="none" />
      <circle cx="597" cy="222" r="9" fill={t.sage} />
      <path d="M634 150 C648 146 656 150 664 156" fill="none" strokeWidth="2" />
      <path d="M740 232 C840 172 960 252 1080 198" fill="none" strokeDasharray="2 12" />
      <path d="M1068 188 L1086 196 L1072 210" fill="none" />
      <rect x="1150" y="150" width="120" height="96" rx="4" fill={t.card} />
      <path d="M1150 176 H1270 M1210 150 V176" fill="none" />
      <rect x="1170" y="92" width="86" height="60" rx="4" fill={t.blobA} />
      <path d="M1170 110 H1256" fill="none" />
      <path d="M1128 70 L1140 256 H1290" fill="none" strokeWidth="3.4" />
      <circle cx="1150" cy="266" r="16" fill={t.card} />
      <circle cx="1150" cy="266" r="5" fill="none" />
      </g>
      <g transform="rotate(-10 700 168)">
      <rect x="662" y="152" width="96" height="32" rx="5" fill={t.card} stroke={t.ink} strokeWidth="2.2" />
      <circle cx="673" cy="168" r="3.5" fill={t.ink} />
      <text x="683" y="174" fontFamily="SFMono-Regular, Menlo, Consolas, monospace" fontSize="15" fontWeight="700" fill={t.ink}>00412</text>
      </g>
      <text x="1176" y="218" fontFamily="Caveat, cursive" fontSize="26" fontWeight="600" fill={t.ink}>Depósito</text>
      <g fill={t.ink}><path d="M900 60 L903 72 L915 75 L903 78 L900 90 L897 78 L885 75 L897 72 Z" /><path d="M150 200 L152 208 L160 210 L152 212 L150 220 L148 212 L140 210 L148 208 Z" /></g>
      </g>
      </>)}
      
      {/* CALENDÁRIO: calendário da semana com turnos marcados, relógio, sol e lua */}
      {cena === 'calendario' && (<>
      <g>
      <path d="M0 262 C300 244 700 272 1000 256 C1200 248 1340 260 1440 252 L1440 300 L0 300 Z" fill={t.sage} opacity="0.55" />
      <g transform="translate(6 6)">
      <rect x="420" y="50" width="420" height="210" rx="12" fill={t.blobB} />
      <circle cx="1060" cy="154" r="66" fill={t.blobB} />
      </g>
      <g stroke={t.ink} strokeWidth="2.6" strokeLinejoin="round" strokeLinecap="round">
      <rect x="420" y="50" width="420" height="210" rx="12" fill={t.card} />
      <rect x="430" y="106" width="64" height="36" rx="5" fill={t.blobA} />
      <rect x="598" y="160" width="64" height="36" rx="5" fill={t.sage} />
      <rect x="682" y="106" width="64" height="36" rx="5" fill={t.blobA} />
      <rect x="766" y="214" width="64" height="36" rx="5" fill={t.sage} />
      <rect x="514" y="214" width="64" height="36" rx="5" fill={t.blobA} />
      <path d="M420 96 H840" fill="none" />
      <path d="M504 96 V260 M588 96 V260 M672 96 V260 M756 96 V260 M420 151 H840 M420 205 H840" fill="none" opacity="0.45" />
      <path d="M480 38 V64 M780 38 V64" fill="none" strokeWidth="3.6" />
      <circle cx="1060" cy="154" r="66" fill={t.blobA} />
      <circle cx="1060" cy="154" r="52" fill={t.card} />
      <path d="M1060 108 V116 M1060 192 V200 M1014 154 H1022 M1098 154 H1106" fill="none" />
      <path d="M1060 154 V122 M1060 154 L1084 168" fill="none" strokeWidth="3.4" />
      <circle cx="230" cy="120" r="32" fill={t.blobA} />
      <path d="M230 72 V62 M230 178 V168 M182 120 H172 M288 120 H278 M196 86 L189 79 M271 161 L264 154 M196 154 L189 161 M271 79 L264 86" fill="none" />
      <path d="M1280 76 A30 30 0 1 0 1308 118 A23 23 0 1 1 1280 76 Z" fill={t.card} />
      </g>
      <g fill={t.ink} fontFamily="Fraunces, Georgia, serif" fontSize="17" fontWeight="600" textAnchor="middle">
      <text x="462" y="81">Seg</text><text x="546" y="81">Ter</text><text x="630" y="81">Qua</text><text x="714" y="81">Qui</text><text x="798" y="81">Sex</text>
      </g>
      <g fill={t.ink}><path d="M1360 170 L1362 178 L1370 180 L1362 182 L1360 190 L1358 182 L1350 180 L1358 178 Z" /><path d="M920 60 L923 72 L935 75 L923 78 L920 90 L917 78 L905 75 L917 72 Z" /></g>
      </g>
      </>)}
      
    </>
  )
}
