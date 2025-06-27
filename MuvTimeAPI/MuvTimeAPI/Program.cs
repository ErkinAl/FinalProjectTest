using MuvTimeAPI.Services;
using Supabase;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllers();
// Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Configure Supabase with SERVICE KEY for bypassing RLS
var supabaseUrl = builder.Configuration["Supabase:Url"] ?? throw new InvalidOperationException("Supabase URL not configured");
var supabaseServiceKey = builder.Configuration["Supabase:ServiceKey"] ?? throw new InvalidOperationException("Supabase Service Key not configured");

builder.Services.AddScoped<Client>(_ =>
{
    var options = new SupabaseOptions
    {
        AutoRefreshToken = false,
        AutoConnectRealtime = false
    };
    
    // Use SERVICE KEY to bypass RLS
    return new Client(supabaseUrl, supabaseServiceKey, options);
});

// Register services
builder.Services.AddScoped<IStatsService, StatsService>();

// Add CORS
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAndroid",
        policy =>
        {
            policy.AllowAnyOrigin()
                  .AllowAnyMethod()
                  .AllowAnyHeader();
        });
});

var app = builder.Build();

// Configure the HTTP request pipeline.
// Enable Swagger in production for Azure (for testing)
app.UseSwagger();
app.UseSwaggerUI();

// Don't force HTTPS redirect in Azure (causes issues)
if (app.Environment.IsDevelopment())
{
    app.UseHttpsRedirection();
}

app.UseCors("AllowAndroid");

app.UseAuthorization();

app.MapControllers();

app.Run();
