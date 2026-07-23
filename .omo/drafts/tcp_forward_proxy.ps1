param(
    [int]$ListenPort = 17897,
    [string]$TargetHost = "127.0.0.1",
    [int]$TargetPort = 7897
)

Add-Type -TypeDefinition @"
using System;
using System.Net;
using System.Net.Sockets;
using System.Threading.Tasks;

public static class OmoTcpForwarder
{
    public static async Task RunAsync(int listenPort, string targetHost, int targetPort)
    {
        var listener = new TcpListener(IPAddress.Any, listenPort);
        listener.Start();
        while (true)
        {
            var inbound = await listener.AcceptTcpClientAsync();
            _ = ForwardAsync(inbound, targetHost, targetPort);
        }
    }

    private static async Task ForwardAsync(TcpClient inbound, string targetHost, int targetPort)
    {
        using (inbound)
        using (var outbound = new TcpClient())
        {
            await outbound.ConnectAsync(targetHost, targetPort);
            using (var input = inbound.GetStream())
            using (var output = outbound.GetStream())
            {
                var upstream = input.CopyToAsync(output);
                var downstream = output.CopyToAsync(input);
                await Task.WhenAny(upstream, downstream);
            }
        }
    }
}
"@

[OmoTcpForwarder]::RunAsync($ListenPort, $TargetHost, $TargetPort).GetAwaiter().GetResult()
