package com.giansantos.bytecodeagent.service.extract;

import java.io.IOException;
import java.util.List;

/** Estrategia de extracao de codigo (bytecode desmontado ou fonte decompilado) a partir de um artefato binario. */
public interface CodeExtractor {

    /** Se este extrator sabe lidar com o arquivo, pelo nome/extensao. */
    boolean supports(String filename);

    /**
     * Extrai ate {@code maxUnits} unidades de codigo do artefato.
     *
     * @param fileBytes conteudo bruto do arquivo enviado
     * @param filename  nome original do arquivo (usado para decidir o formato)
     * @param maxUnits  numero maximo de unidades a extrair
     */
    List<CodeUnit> extract(byte[] fileBytes, String filename, int maxUnits) throws IOException;
}
