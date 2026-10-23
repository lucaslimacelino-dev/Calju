"use strict";

// Página de exclusão da conta: pede um código por e-mail e confirma com ele.
const pedido = document.getElementById("pedido");
const confirmacao = document.getElementById("confirmacao");
const mensagem = document.getElementById("mensagem");
const email = document.getElementById("email");
const codigo = document.getElementById("codigo");

function mostrar(texto, tipo) {
    mensagem.textContent = texto;
    mensagem.className = "aviso " + (tipo || "");
}

async function enviar(caminho, corpo) {
    const resposta = await fetch("/api/v1/auth/" + caminho, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(corpo),
    });
    if (resposta.status === 429) {
        throw new Error("Muitas tentativas. Espere um pouco e tente de novo.");
    }
    if (resposta.status === 400) {
        throw new Error(caminho.endsWith("confirm")
            ? "Código inválido ou vencido. Confira o e-mail ou peça outro código."
            : "Confira o e-mail digitado.");
    }
    if (!resposta.ok) {
        throw new Error("O servidor não respondeu direito. Tente de novo em instantes.");
    }
}

async function tratar(evento, form, acao) {
    evento.preventDefault();
    const botao = form.querySelector("button");
    botao.disabled = true;
    mostrar("");
    try {
        await acao();
    } catch (erro) {
        mostrar(erro.message, "erro");
    } finally {
        botao.disabled = false;
    }
}

pedido.addEventListener("submit", (evento) => tratar(evento, pedido, async () => {
    await enviar("account-deletion", { email: email.value.trim() });
    confirmacao.hidden = false;
    codigo.focus();
}));

confirmacao.addEventListener("submit", (evento) => tratar(evento, confirmacao, async () => {
    await enviar("account-deletion/confirm", { email: email.value.trim(), code: codigo.value.trim() });
    pedido.hidden = true;
    confirmacao.hidden = true;
    mostrar("Pronto: a conta e os dados sincronizados foram apagados.", "ok");
}));
